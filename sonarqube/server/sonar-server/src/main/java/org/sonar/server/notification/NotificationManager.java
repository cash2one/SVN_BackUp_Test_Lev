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
package org.sonar.server.notification;

import com.google.common.collect.Multimap;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;

/**
 * The notification manager receives notifications and is in charge of storing them so that they are processed by the notification service.
 * <p>
 * Pico provides an instance of this class, and plugins just need to create notifications and pass them to this manager with
 * the {@link NotificationManager#scheduleForSending(Notification)} method.
 * </p>
 */
public interface NotificationManager {

  /**
   * Receives a notification and stores it so that it is processed by the notification service.
   *
   * @param notification the notification.
   */
  void scheduleForSending(Notification notification);

  /**
   * Receives notifications and stores them so that they are processed by the notification service.
   *
   * @param notifications the notifications.
   * @since 3.7.1
   */
  void scheduleForSending(List<Notification> notifications);

  /**
   * <p>
   * Returns the list of users who subscribed to the given dispatcher, along with the notification channels (email, twitter, ...) that they choose
   * for this dispatcher.
   * </p>
   * <p>
   * The resource ID can be null in case of notifications that have nothing to do with a specific project (like system notifications).
   * </p>
   *
   * @param dispatcher the dispatcher for which this list of users is requested
   * @param projectUuid UUID of the project
   * @return the list of user login along with the subscribed channels
   */
  Multimap<String, NotificationChannel> findSubscribedRecipientsForDispatcher(NotificationDispatcher dispatcher, String projectUuid);

  Multimap<String, NotificationChannel> findNotificationSubscribers(NotificationDispatcher dispatcher, @Nullable String componentKey);
}
