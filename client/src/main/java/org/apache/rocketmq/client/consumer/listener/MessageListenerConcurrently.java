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
package org.apache.rocketmq.client.consumer.listener;

import java.util.List;
import org.apache.rocketmq.common.message.MessageExt;

/**
 * A MessageListenerConcurrently object is used to receive asynchronously delivered messages concurrently
 *
 * MessageListenerConcurrently对象用于同时接收异步传递的消息
 */
public interface MessageListenerConcurrently extends MessageListener {
    /**
     * It is not recommend to throw exception,rather than returning ConsumeConcurrentlyStatus.RECONSUME_LATER if
     * consumption failure
     *
     * 不推荐抛异常，如果消费失败，返回ConsumeConcurrentlyStatus.RECONSUME_LATER
     *
     * 一般使用的时候，都是实现此方法，消费消息，返回状态码
     *
     * @param msgs msgs.size() >= 1<br> DefaultMQPushConsumer.consumeMessageBatchMaxSize=1,you can modify here
     *             msgs.size()>=1<br> DefaultMQPushConsumer.consumeMessageBatchMaxSize=1,
     *             你可以修改每次消费消息的数量，默认设置是每次消费一条,
     *             consumeMessageBatchMaxSize 这个size是消费者注册的回调listener一次处理的消息数，默认是1.
     *             不是每次拉取的消息数pullBatchSize（默认是32），这个不要搞混。
     * @return The consume status 返回消息状态
     */
    ConsumeConcurrentlyStatus consumeMessage(final List<MessageExt> msgs,
        final ConsumeConcurrentlyContext context);
}
