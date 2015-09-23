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

package org.sonar.server.computation.component;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;
import static org.sonar.server.computation.component.ComponentVisitor.Order.PRE_ORDER;

public class ReportVisitorsCrawlerTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final Component FILE_5 = component(FILE, 5);
  private static final Component DIRECTORY_4 = component(DIRECTORY, 4, FILE_5);
  private static final Component MODULE_3 = component(MODULE, 3, DIRECTORY_4);
  private static final Component MODULE_2 = component(MODULE, 2, MODULE_3);
  private static final Component COMPONENT_TREE = component(PROJECT, 1, MODULE_2);

  private final TypeAwareVisitor spyPreOrderTypeAwareVisitor = spy(new TestTypeAwareVisitor(CrawlerDepthLimit.FILE, PRE_ORDER));
  private final TypeAwareVisitor spyPostOrderTypeAwareVisitor = spy(new TestTypeAwareVisitor(CrawlerDepthLimit.FILE, POST_ORDER));
  private final TestPathAwareVisitor spyPathAwareVisitor = spy(new TestPathAwareVisitor(CrawlerDepthLimit.FILE, POST_ORDER));

  @Test
  public void execute_each_visitor_on_each_level() throws Exception {
    InOrder inOrder = inOrder(spyPostOrderTypeAwareVisitor, spyPathAwareVisitor);
    VisitorsCrawler underTest = new VisitorsCrawler(Arrays.asList(spyPostOrderTypeAwareVisitor, spyPathAwareVisitor));
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitAny(FILE_5);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitFile(FILE_5);
    inOrder.verify(spyPathAwareVisitor).visitAny(eq(FILE_5), any(PathAwareVisitor.Path.class));
    inOrder.verify(spyPathAwareVisitor).visitFile(eq(FILE_5), any(PathAwareVisitor.Path.class));

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitAny(DIRECTORY_4);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitDirectory(DIRECTORY_4);
    inOrder.verify(spyPathAwareVisitor).visitAny(eq(DIRECTORY_4), any(PathAwareVisitor.Path.class));
    inOrder.verify(spyPathAwareVisitor).visitDirectory(eq(DIRECTORY_4), any(PathAwareVisitor.Path.class));

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitAny(MODULE_3);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitModule(MODULE_3);
    inOrder.verify(spyPathAwareVisitor).visitAny(eq(MODULE_3), any(PathAwareVisitor.Path.class));
    inOrder.verify(spyPathAwareVisitor).visitModule(eq(MODULE_3), any(PathAwareVisitor.Path.class));

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitAny(MODULE_2);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitModule(MODULE_2);
    inOrder.verify(spyPathAwareVisitor).visitAny(eq(MODULE_2), any(PathAwareVisitor.Path.class));
    inOrder.verify(spyPathAwareVisitor).visitModule(eq(MODULE_2), any(PathAwareVisitor.Path.class));

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitAny(COMPONENT_TREE);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitProject(COMPONENT_TREE);
    inOrder.verify(spyPathAwareVisitor).visitAny(eq(COMPONENT_TREE), any(PathAwareVisitor.Path.class));
    inOrder.verify(spyPathAwareVisitor).visitProject(eq(COMPONENT_TREE), any(PathAwareVisitor.Path.class));
  }

  @Test
  public void execute_pre_visitor_before_post_visitor() throws Exception {
    InOrder inOrder = inOrder(spyPreOrderTypeAwareVisitor, spyPostOrderTypeAwareVisitor);
    VisitorsCrawler underTest = new VisitorsCrawler(Arrays.<ComponentVisitor>asList(spyPreOrderTypeAwareVisitor, spyPostOrderTypeAwareVisitor));
    underTest.visit(COMPONENT_TREE);

    inOrder.verify(spyPreOrderTypeAwareVisitor).visitProject(COMPONENT_TREE);
    inOrder.verify(spyPreOrderTypeAwareVisitor).visitModule(MODULE_2);
    inOrder.verify(spyPreOrderTypeAwareVisitor).visitModule(MODULE_3);
    inOrder.verify(spyPreOrderTypeAwareVisitor).visitDirectory(DIRECTORY_4);
    inOrder.verify(spyPreOrderTypeAwareVisitor).visitFile(FILE_5);

    inOrder.verify(spyPostOrderTypeAwareVisitor).visitFile(FILE_5);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitDirectory(DIRECTORY_4);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitModule(MODULE_3);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitModule(MODULE_2);
    inOrder.verify(spyPostOrderTypeAwareVisitor).visitProject(COMPONENT_TREE);
  }

  @Test
  public void fail_with_IAE_when_visitor_is_not_path_aware_or_type_aware() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Only TypeAwareVisitor and PathAwareVisitor can be used");

    ComponentVisitor componentVisitor = new ComponentVisitor() {
      @Override
      public Order getOrder() {
        return PRE_ORDER;
      }

      @Override
      public CrawlerDepthLimit getMaxDepth() {
        return CrawlerDepthLimit.FILE;
      }
    };
    new VisitorsCrawler(Arrays.asList(componentVisitor));
  }

  private static Component component(final Component.Type type, final int ref, final Component... children) {
    return ReportComponent.builder(type, ref).addChildren(children).build();
  }

  private static class TestTypeAwareVisitor extends TypeAwareVisitorAdapter {

    public TestTypeAwareVisitor(CrawlerDepthLimit maxDepth, ComponentVisitor.Order order) {
      super(maxDepth, order);
    }
  }

  private static class TestPathAwareVisitor extends PathAwareVisitorAdapter<Integer> {

    public TestPathAwareVisitor(CrawlerDepthLimit maxDepth, ComponentVisitor.Order order) {
      super(maxDepth, order, new SimpleStackElementFactory<Integer>() {
        @Override
        public Integer createForAny(Component component) {
          return component.getReportAttributes().getRef();
        }
      });
    }
  }

}
