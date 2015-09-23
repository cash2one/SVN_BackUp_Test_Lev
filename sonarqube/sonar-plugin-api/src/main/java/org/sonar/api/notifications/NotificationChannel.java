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
package org.sonar.api.notifications;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * <p>
 * Plugins should extend this class to provide implementation on a specific way to deliver notifications.
 * </p>
 * For example:
 * <ul>
 * <li>email - sends email as soon as possible</li>
 * <li>email (digest) - collects notifications and sends them together once a day</li>
 * <li>gtalk - sends a chat message as soon as possible</li>
 * </ul>
 * 
 * @since 2.10
 */
@ServerSide
@ExtensionPoint
public abstract class NotificationChannel {

  /**
   * Returns the unique key of this channel. 
   * 
   * @return the key
   */
  public String getKey() {
    return getClass().getSimpleName();
  }

  /**
   * Implements the delivery of the given notification to the given user.
   * 
   * @param notification the notification to deliver
   * @param userlogin the login of the user who should receive the notification
   */
  public abstract void deliver(Notification notification, String userlogin);

  @Override
  public String toString() {
    return getKey();
  }

}
