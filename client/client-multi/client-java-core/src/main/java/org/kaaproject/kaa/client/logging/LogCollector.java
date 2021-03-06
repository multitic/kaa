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

package org.kaaproject.kaa.client.logging;

import javax.annotation.Generated;

import org.kaaproject.kaa.schema.base.Log;

/**
 * <p>Interface for a log collector.</p>
 *
 * <p>Adds new log record to a local storage.</p>
 *
 * <p>May be configured by setting user defined log record storage,
 * storage status, upload configuration and log upload strategy.
 * Each of them may be set independently of others.</p>
 *
 * <p>Reference implementation of each module used by default.</p>
 * 
 * <p>This interface is auto-generated.</p>
 *
 * @see LogStorage
 * @see LogStorageStatus
 * @see LogUploadStrategy
 * @see LogUploadConfiguration
 */
@Generated("LogCollector.java.template")
public interface LogCollector extends GenericLogCollector{

    /**
     * Adds new log record to local storage.
     *
     * @param record New log record object
     */
    void addLogRecord(Log record);
}