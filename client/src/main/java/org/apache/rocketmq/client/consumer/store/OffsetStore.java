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
package org.apache.rocketmq.client.consumer.store;

import java.util.Map;
import java.util.Set;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.remoting.exception.RemotingException;

/**
 * Offset store interface
 * 消息偏移量 offset接口
 */
public interface OffsetStore {
    /**
     * Load
     * 导入消息偏移量到内存中
     */
    void load() throws MQClientException;

    /**
     * Update the offset,store it in memory
     * 修改偏移量，并存储到内存中
     */
    void updateOffset(final MessageQueue mq, final long offset, final boolean increaseOnly);

    /**
     * Get offset from local storage
     *  查询本地内存中偏移量offset
     * @return The fetched offset
     */
    long readOffset(final MessageQueue mq, final ReadOffsetType type);

    /**
     * Persist all offsets,may be in local storage or remote name server
     * 持久化所有消息偏移量到本地磁盘或者远程服务器（TODO 暂时不确定是远程broker服务器还是name server服务器）
     */
    void persistAll(final Set<MessageQueue> mqs);

    /**
     * Persist the offset,may be in local storage or remote name server
     * 持久化单个消息偏移量到本地磁盘或者远程服务器（TODO 暂时不确定是远程broker服务器还是name server服务器）
     */
    void persist(final MessageQueue mq);

    /**
     * Remove offset
     * 移除偏移量offset
     */
    void removeOffset(MessageQueue mq);

    /**
     * @return The cloned offset table of given topic
     *          通过指定的topic返回对应的偏移量集合
     */
    Map<MessageQueue, Long> cloneOffsetTable(String topic);

    /**
     * Update the Consumer Offset synchronously, once the Master is off, updated to Slave,
     * here need to be optimized.
     *
     * 同步修改消费者偏移量，一旦主 broker master宕机，更新到slave broker,这段代码需要被优化
     * @param mq
     * @param offset
     * @param isOneway
     */
    void updateConsumeOffsetToBroker(MessageQueue mq, long offset, boolean isOneway) throws RemotingException,
        MQBrokerException, InterruptedException, MQClientException;
}
