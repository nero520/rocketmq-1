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
package org.apache.rocketmq.common.message;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.common.UtilAll;

public class MessageClientIDSetter {
    private static final String TOPIC_KEY_SPLITTER = "#";
    private static final int LEN;
    private static final String FIX_STRING;
    private static final AtomicInteger COUNTER;
    private static long startTime;
    private static long nextStartTime;

    //初始化参数LEN,FIX_STRING,COUNTER
    static {
        LEN = 4 + 2 + 4 + 4 + 2;
        ByteBuffer tempBuffer = ByteBuffer.allocate(10);
        tempBuffer.position(2);
        tempBuffer.putInt(UtilAll.getPid());
        tempBuffer.position(0);
        try {
            tempBuffer.put(UtilAll.getIP());
        } catch (Exception e) {
            tempBuffer.put(createFakeIP());
        }
        tempBuffer.position(6);
        tempBuffer.putInt(MessageClientIDSetter.class.getClassLoader().hashCode());
        FIX_STRING = UtilAll.bytes2string(tempBuffer.array());
        setStartTime(System.currentTimeMillis());
        COUNTER = new AtomicInteger(0);
    }

    /**
     * 设置开始时间
     * @param millis
     */
    private synchronized static void setStartTime(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        startTime = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, 1);
        nextStartTime = cal.getTimeInMillis();
    }

    public static Date getNearlyTimeFromID(String msgID) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        byte[] bytes = UtilAll.string2bytes(msgID);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.put(bytes, 10, 4);
        buf.position(0);
        long spanMS = buf.getLong();
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monStartTime = cal.getTimeInMillis();
        if (monStartTime + spanMS >= now) {
            cal.add(Calendar.MONTH, -1);
            monStartTime = cal.getTimeInMillis();
        }
        cal.setTimeInMillis(monStartTime + spanMS);
        return cal.getTime();
    }

    public static String getIPStrFromID(String msgID) {
        byte[] ipBytes = getIPFromID(msgID);
        return UtilAll.ipToIPv4Str(ipBytes);
    }

    public static byte[] getIPFromID(String msgID) {
        byte[] result = new byte[4];
        byte[] bytes = UtilAll.string2bytes(msgID);
        System.arraycopy(bytes, 0, result, 0, 4);
        return result;
    }

    /**
     * 最终生成msgId
     *
     * msgId的生成因子是: ip + 进程pid + MessageClientIDSetter.class.getClassLoader().hashCode() + time + counter(AtomicInteger自增变量）
     * 最后调用bytes2string进行十六进制的移位和编码就产生了我们的msgId
     *
     * 每个producer实例来说ip都是唯一的，所以不同producer生成的msgId是不会重复的
     * 对于producer单个实例来说的区分因子是：time + counter
     * 首先应用不重启的情况下msgId是保证唯一性的，应用重启了只要系统的时钟不变msgId也是唯一的
     *
     * 所以只要系统的时钟不回拨我们就可以保证msgId的全局唯一
     *
     * 有人也许会说应用运行了一个月再进行重启msgId就会重复了。从生成算法上来说是的！但是MQ的message是有时效性的，有效期是72小时也就是3天。
     *
     * 每天的凌晨4点rocketMQ会把过期的message清除掉。所以msgId也是保证全局唯一的。
     *
     * @return
     */
    public static String createUniqID() {
        //msgId的长度是LEN * 2 = 16 * 2 = 32
        StringBuilder sb = new StringBuilder(LEN * 2);
        //FIX_STRING = ip + 进程pid + MessageClientIDSetter.class.getClassLoader().hashCode()
        sb.append(FIX_STRING);
        //createUniqIDBuffer 加入time 和 counter 因子
        sb.append(UtilAll.bytes2string(createUniqIDBuffer()));
        return sb.toString();
    }

    /**
     * 初始化buffer createUniqIDBuffer 加入time 和 counter 因子
     * @return
     */
    private static byte[] createUniqIDBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(4 + 2);
        long current = System.currentTimeMillis();
        if (current >= nextStartTime) {
            setStartTime(current);
        }
        buffer.position(0);
        buffer.putInt((int) (System.currentTimeMillis() - startTime));
        buffer.putShort((short) COUNTER.getAndIncrement());
        return buffer.array();
    }

    public static void setUniqID(final Message msg) {
        if (msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX) == null) {
            msg.putProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX, createUniqID());
        }
    }

    public static String getUniqID(final Message msg) {
        return msg.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
    }

    public static byte[] createFakeIP() {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(System.currentTimeMillis());
        bb.position(4);
        byte[] fakeIP = new byte[4];
        bb.get(fakeIP);
        return fakeIP;
    }
}
    
