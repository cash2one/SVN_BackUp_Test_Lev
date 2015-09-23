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
package org.sonar.batch.issue.tracking;

import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.fs.internal.FileMetadata.LineHashConsumer;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import javax.annotation.Nullable;

import java.util.Collection;

/**
 * Wraps a {@link Sequence} to assign hash codes to elements.
 */
public final class FileHashes {

  private final String[] hashes;
  private final Multimap<String, Integer> linesByHash;

  private FileHashes(String[] hashes, Multimap<String, Integer> linesByHash) {
    this.hashes = hashes;
    this.linesByHash = linesByHash;
  }

  public static FileHashes create(String[] hashes) {
    int size = hashes.length;
    Multimap<String, Integer> linesByHash = LinkedHashMultimap.create();
    for (int i = 0; i < size; i++) {
      // indices in array are shifted one line before
      linesByHash.put(hashes[i], i + 1);
    }
    return new FileHashes(hashes, linesByHash);
  }

  public static FileHashes create(DefaultInputFile f) {
    final byte[][] hashes = new byte[f.lines()][];
    FileMetadata.computeLineHashesForIssueTracking(f, new LineHashConsumer() {

      @Override
      public void consume(int lineIdx, @Nullable byte[] hash) {
        hashes[lineIdx - 1] = hash;
      }
    });

    int size = hashes.length;
    Multimap<String, Integer> linesByHash = LinkedHashMultimap.create();
    String[] hexHashes = new String[size];
    for (int i = 0; i < size; i++) {
      String hash = hashes[i] != null ? Hex.encodeHexString(hashes[i]) : "";
      hexHashes[i] = hash;
      // indices in array are shifted one line before
      linesByHash.put(hash, i + 1);
    }
    return new FileHashes(hexHashes, linesByHash);
  }

  public int length() {
    return hashes.length;
  }

  public Collection<Integer> getLinesForHash(String hash) {
    return linesByHash.get(hash);
  }

  public String getHash(int line) {
    // indices in array are shifted one line before
    return (String) ObjectUtils.defaultIfNull(hashes[line - 1], "");
  }
}
