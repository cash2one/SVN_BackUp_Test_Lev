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
package org.sonar.core.util;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.codec.binary.Base64;

/**
 * Heavily inspired from Elasticsearch {@code TimeBasedUUIDGenerator}, which could be directly
 * used the day {@code UuidFactoryImpl} is moved outside module sonar-core.
 * See https://github.com/elastic/elasticsearch/blob/master/core/src/main/java/org/elasticsearch/common/TimeBasedUUIDGenerator.java
 */
public enum UuidFactoryImpl implements UuidFactory {

  /**
   * Should be removed as long {@link Uuids} is not used anymore. {@code UuidFactoryImpl}
   * should be built by picocontainer through a public constructor.
   */
  INSTANCE;

  // We only use bottom 3 bytes for the sequence number. Paranoia: init with random int so that if JVM/OS/machine goes down, clock slips
  // backwards, and JVM comes back up, we are less likely to be on the same sequenceNumber at the same time:
  private final AtomicInteger sequenceNumber = new AtomicInteger(new SecureRandom().nextInt());

  // Used to ensure clock moves forward
  private long lastTimestamp = 0L;

  private final byte[] secureMungedAddress = MacAddressProvider.getSecureMungedAddress();

  @Override
  public String create() {
    int sequenceId = sequenceNumber.incrementAndGet() & 0xffffff;
    long timestamp = System.currentTimeMillis();

    synchronized (this) {
      // Don't let timestamp go backwards, at least "on our watch" (while this JVM is running). We are still vulnerable if we are
      // shut down, clock goes backwards, and we restart... for this we randomize the sequenceNumber on init to decrease chance of
      // collision:
      timestamp = Math.max(lastTimestamp, timestamp);

      if (sequenceId == 0) {
        // Always force the clock to increment whenever sequence number is 0, in case we have a long time-slip backwards:
        timestamp++;
      }

      lastTimestamp = timestamp;
    }

    byte[] uuidBytes = new byte[15];

    // Only use lower 6 bytes of the timestamp (this will suffice beyond the year 10000):
    putLong(uuidBytes, timestamp, 0, 6);

    // MAC address adds 6 bytes:
    System.arraycopy(secureMungedAddress, 0, uuidBytes, 6, secureMungedAddress.length);

    // Sequence number adds 3 bytes:
    putLong(uuidBytes, sequenceId, 12, 3);

    return Base64.encodeBase64URLSafeString(uuidBytes);
  }

  /** Puts the lower numberOfLongBytes from l into the array, starting index pos. */
  private static void putLong(byte[] array, long l, int pos, int numberOfLongBytes) {
    for (int i = 0; i < numberOfLongBytes; ++i) {
      array[pos + numberOfLongBytes - i - 1] = (byte) (l >>> (i * 8));
    }
  }
}
