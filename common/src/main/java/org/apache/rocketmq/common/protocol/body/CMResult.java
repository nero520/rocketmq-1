/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.common.protocol.body;

public enum CMResult {
    /**
     * 消费成功
     */
    CR_SUCCESS,
    /**
     * 推迟消费
     */
    CR_LATER,
    /**
     * 事务消息回滚
     */
    CR_ROLLBACK,
    /**
     * 事务消息投递
     */
    CR_COMMIT,
    /**
     * 消费过程异常
     */
    CR_THROW_EXCEPTION,
    /**
     * 消费结果状态为null
     */
    CR_RETURN_NULL,
}
