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
package org.apache.rocketmq.logappender.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common Producer component
 * 消息发送者实例组件
 */
public class ProducerInstance {

    /**
     * 自定义properties的属性APPENDER_TYPE
     */
    public static final String APPENDER_TYPE = "APPENDER_TYPE";

    /**
     * 自定义properties的属性APPENDER_TYPE对应的默认值LOG4J_APPENDER
     */
    public static final String LOG4J_APPENDER = "LOG4J_APPENDER";

    public static final String LOG4J2_APPENDER = "LOG4J2_APPENDER";

    public static final String LOGBACK_APPENDER = "LOGBACK_APPENDER";

    /**
     * 默认分组
     */
    public static final String DEFAULT_GROUP = "rocketmq_appender";

    /**
     * 缓存的生产者
     */
    private ConcurrentHashMap</*nameServerAddress + "_" + group*/String, MQProducer> producerMap = new ConcurrentHashMap<String, MQProducer>();

    private static ProducerInstance instance = new ProducerInstance();

    public static ProducerInstance getProducerInstance() {
        return instance;
    }

    /**
     *  获取组合key nameServerAddress + "_" + group
     * @param nameServerAddress nameServer地址
     * @param group 分组
     * @return
     */
    private String genKey(String nameServerAddress, String group) {
        return nameServerAddress + "_" + group;
    }

    /**
     * 得到实例化
     * @param nameServerAddress
     * @param group
     * @return
     * @throws MQClientException
     */
    public MQProducer getInstance(String nameServerAddress, String group) throws MQClientException {
        if (StringUtils.isBlank(group)) {
            group = DEFAULT_GROUP;
        }

        //1.获取
        String genKey = genKey(nameServerAddress, group);
        MQProducer p = getProducerInstance().producerMap.get(genKey);
        if (p != null) {
            return p;
        }

        DefaultMQProducer defaultMQProducer = new DefaultMQProducer(group);
        defaultMQProducer.setNamesrvAddr(nameServerAddress);
        MQProducer beforeProducer = null;
        beforeProducer = getProducerInstance().producerMap.putIfAbsent(genKey, defaultMQProducer);
        if (beforeProducer != null) {
            return beforeProducer;
        }
        defaultMQProducer.start();
        return defaultMQProducer;
    }

    /**
     *  移除mq消息发送者
     * @param nameServerAddress
     * @param group
     */
    public void removeAndClose(String nameServerAddress, String group) {
        if (group == null) {
            group = DEFAULT_GROUP;
        }
        String genKey = genKey(nameServerAddress, group);
        MQProducer producer = getProducerInstance().producerMap.remove(genKey);

        if (producer != null) {
            producer.shutdown();
        }
    }

    public void closeAll() {
        Set<Map.Entry<String, MQProducer>> entries = getProducerInstance().producerMap.entrySet();
        for (Map.Entry<String, MQProducer> entry : entries) {
            getProducerInstance().producerMap.remove(entry.getKey());
            entry.getValue().shutdown();
        }
    }

}
