/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.source.FileSourceDto.Type;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentVisitor;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.ReportTreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;

public class PersistTestsStep implements ComputationStep {

  private static final Logger LOG = Loggers.get(PersistTestsStep.class);

  private final DbClient dbClient;
  private final System2 system;
  private final BatchReportReader reportReader;
  private final ReportTreeRootHolder treeRootHolder;

  public PersistTestsStep(DbClient dbClient, System2 system, BatchReportReader reportReader, ReportTreeRootHolder treeRootHolder) {
    this.dbClient = dbClient;
    this.system = system;
    this.reportReader = reportReader;
    this.treeRootHolder = treeRootHolder;
  }

  @Override
  public void execute() {
    DbSession session = dbClient.openSession(true);
    try {
      TestDepthTraversalTypeAwareVisitor visitor = new TestDepthTraversalTypeAwareVisitor(session);
      new DepthTraversalTypeAwareCrawler(visitor).visit(treeRootHolder.getRoot());
      session.commit();
      if (visitor.hasUnprocessedCoverageDetails) {
        LOG.warn("Some coverage tests are not taken into account during analysis of project '{}'", visitor.getProjectKey());
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public String getDescription() {
    return "Persist tests";
  }

  private class TestDepthTraversalTypeAwareVisitor extends TypeAwareVisitorAdapter {
    final DbSession session;
    final Map<String, FileSourceDto> existingFileSourcesByUuid;
    final String projectUuid;
    final String projectKey;
    boolean hasUnprocessedCoverageDetails = false;

    public TestDepthTraversalTypeAwareVisitor(DbSession session) {
      super(CrawlerDepthLimit.FILE, ComponentVisitor.Order.PRE_ORDER);
      this.session = session;
      this.existingFileSourcesByUuid = new HashMap<>();
      this.projectUuid = treeRootHolder.getRoot().getUuid();
      this.projectKey = treeRootHolder.getRoot().getKey();
      session.select("org.sonar.db.source.FileSourceMapper.selectHashesForProject",
        ImmutableMap.of("projectUuid", treeRootHolder.getRoot().getUuid(), "dataType", Type.TEST),
        new ResultHandler() {
          @Override
          public void handleResult(ResultContext context) {
            FileSourceDto dto = (FileSourceDto) context.getResultObject();
            existingFileSourcesByUuid.put(dto.getFileUuid(), dto);
          }
        });
    }

    @Override
    public void visitFile(Component file) {
      if (file.getFileAttributes().isUnitTest()) {
        persistTestResults(file);
      }
    }

    private void persistTestResults(Component component) {
      Multimap<String, DbFileSources.Test.Builder> testsByName = buildDbTests(component.getReportAttributes().getRef());
      Table<String, String, DbFileSources.Test.CoveredFile.Builder> coveredFilesByName = loadCoverageDetails(component.getReportAttributes().getRef());
      List<DbFileSources.Test> tests = addCoveredFilesToTests(testsByName, coveredFilesByName);
      if (checkIfThereAreUnprocessedCoverageDetails(testsByName, coveredFilesByName, component.getKey())) {
        hasUnprocessedCoverageDetails = true;
      }

      if (tests.isEmpty()) {
        return;
      }

      String componentUuid = getUuid(component.getReportAttributes().getRef());
      FileSourceDto existingDto = existingFileSourcesByUuid.get(componentUuid);
      long now = system.now();
      if (existingDto != null) {
        // update
        existingDto
          .setTestData(tests)
          .setUpdatedAt(now);
        dbClient.fileSourceDao().update(session, existingDto);
      } else {
        // insert
        FileSourceDto newDto = new FileSourceDto()
          .setTestData(tests)
          .setFileUuid(componentUuid)
          .setProjectUuid(projectUuid)
          .setDataType(Type.TEST)
          .setCreatedAt(now)
          .setUpdatedAt(now);
        dbClient.fileSourceDao().insert(session, newDto);
      }
    }

    private boolean checkIfThereAreUnprocessedCoverageDetails(Multimap<String, DbFileSources.Test.Builder> testsByName,
      Table<String, String, DbFileSources.Test.CoveredFile.Builder> coveredFilesByName, String componentKey) {
      Set<String> unprocessedCoverageDetailNames = new HashSet<>(coveredFilesByName.rowKeySet());
      unprocessedCoverageDetailNames.removeAll(testsByName.keySet());
      boolean hasUnprocessedCoverage = !unprocessedCoverageDetailNames.isEmpty();
      if (hasUnprocessedCoverage) {
        LOG.trace("The following test coverages for file '{}' have not been taken into account: {}", componentKey, Joiner.on(", ").join(unprocessedCoverageDetailNames));
      }
      return hasUnprocessedCoverage;
    }

    private List<DbFileSources.Test> addCoveredFilesToTests(Multimap<String, DbFileSources.Test.Builder> testsByName,
      Table<String, String, DbFileSources.Test.CoveredFile.Builder> coveredFilesByName) {
      List<DbFileSources.Test> tests = new ArrayList<>();
      for (DbFileSources.Test.Builder test : testsByName.values()) {
        Collection<DbFileSources.Test.CoveredFile.Builder> coveredFiles = coveredFilesByName.row(test.getName()).values();
        if (!coveredFiles.isEmpty()) {
          for (DbFileSources.Test.CoveredFile.Builder coveredFile : coveredFiles) {
            test.addCoveredFile(coveredFile);
          }
        }
        tests.add(test.build());
      }

      return tests;
    }

    private Multimap<String, DbFileSources.Test.Builder> buildDbTests(int componentRed) {
      Multimap<String, DbFileSources.Test.Builder> tests = ArrayListMultimap.create();

      try (CloseableIterator<BatchReport.Test> testIterator = reportReader.readTests(componentRed)) {
        while (testIterator.hasNext()) {
          BatchReport.Test batchTest = testIterator.next();
          DbFileSources.Test.Builder dbTest = DbFileSources.Test.newBuilder();
          dbTest.setUuid(Uuids.create());
          dbTest.setName(batchTest.getName());
          if (batchTest.hasStacktrace()) {
            dbTest.setStacktrace(batchTest.getStacktrace());
          }
          if (batchTest.hasStatus()) {
            dbTest.setStatus(DbFileSources.Test.TestStatus.valueOf(batchTest.getStatus().name()));
          }
          if (batchTest.hasMsg()) {
            dbTest.setMsg(batchTest.getMsg());
          }
          if (batchTest.hasDurationInMs()) {
            dbTest.setExecutionTimeMs(batchTest.getDurationInMs());
          }

          tests.put(dbTest.getName(), dbTest);
        }
      }

      return tests;
    }

    /**
     * returns a Table of (test name, main file uuid, covered file)
     */
    private Table<String, String, DbFileSources.Test.CoveredFile.Builder> loadCoverageDetails(int testFileRef) {
      Table<String, String, DbFileSources.Test.CoveredFile.Builder> nameToCoveredFiles = HashBasedTable.create();

      try (CloseableIterator<BatchReport.CoverageDetail> coverageIterator = reportReader.readCoverageDetails(testFileRef)) {
        while (coverageIterator.hasNext()) {
          BatchReport.CoverageDetail batchCoverageDetail = coverageIterator.next();
          for (BatchReport.CoverageDetail.CoveredFile batchCoveredFile : batchCoverageDetail.getCoveredFileList()) {
            String testName = batchCoverageDetail.getTestName();
            String mainFileUuid = getUuid(batchCoveredFile.getFileRef());
            DbFileSources.Test.CoveredFile.Builder existingDbCoveredFile = nameToCoveredFiles.get(testName, mainFileUuid);
            List<Integer> batchCoveredLines = batchCoveredFile.getCoveredLineList();
            if (existingDbCoveredFile == null) {
              DbFileSources.Test.CoveredFile.Builder dbCoveredFile = DbFileSources.Test.CoveredFile.newBuilder()
                .setFileUuid(getUuid(batchCoveredFile.getFileRef()))
                .addAllCoveredLine(batchCoveredLines);
              nameToCoveredFiles.put(testName, mainFileUuid, dbCoveredFile);
            } else {
              List<Integer> remainingBatchCoveredLines = new ArrayList<>(batchCoveredLines);
              remainingBatchCoveredLines.removeAll(existingDbCoveredFile.getCoveredLineList());
              existingDbCoveredFile.addAllCoveredLine(batchCoveredLines);
            }
          }
        }
      }
      return nameToCoveredFiles;
    }

    private String getUuid(int fileRef) {
      return treeRootHolder.getComponentByRef(fileRef).getUuid();
    }

    public String getProjectKey() {
      return projectKey;
    }
  }

}
