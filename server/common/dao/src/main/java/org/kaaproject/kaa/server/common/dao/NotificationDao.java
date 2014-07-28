/*
 * Copyright 2014 CyberVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kaaproject.kaa.server.common.dao;

import java.util.List;

import org.kaaproject.kaa.common.dto.NotificationTypeDto;

/**
 * The Interface NotificationDao.
 *
 * @param <T> the generic type
 */
public interface NotificationDao<T> extends Dao<T> {

    /**
     * Find notifications by schema id.
     *
     * @param schemaId the schema id
     * @return the list of notifications
     */
    List<T> findNotificationsBySchemaId(String schemaId);

    /**
     * Removes the notifications by schema id.
     *
     * @param schemaId the schema id
     */
    void removeNotificationsBySchemaId(String schemaId);

    /**
     * Find notifications by application id.
     *
     * @param appId the application id
     * @return the list of notifications
     */
    List<T> findNotificationsByAppId(String appId);

    /**
     * Removes the notifications by application id.
     *
     * @param appId the application id
     */
    void removeNotificationsByAppId(String appId);

    /**
     * Find notifications by topic id.
     *
     * @param topicId the topic id
     * @return the list of notifications
     */
    List<T> findNotificationsByTopicId(String topicId);

    /**
     * Removes the notifications by topic id.
     *
     * @param topicId the topic id
     */
    void removeNotificationsByTopicId(String topicId);

    /**
     * Find notifications by schema id and type.
     *
     * @param schemaId the schema id
     * @param type the type of notification
     * @return the list of notifications
     */
    List<T> findNotificationsBySchemaIdAndType(String schemaId, NotificationTypeDto type);

    /**
     * Find notifications by topic id,
     * notification schema version and start sequence number.
     *
     * @param topicId the topic id
     * @param seqNum the sequence number
     * @param sysNfVersion the system notification version
     * @param userNfVersion the user notification version
     * @return the list of notifications
     */
    List<T> findNotificationsByTopicIdAndVersionAndStartSecNum(String topicId, int seqNum, int sysNfVersion, int userNfVersion);
}