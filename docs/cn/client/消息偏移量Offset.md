1.什么是offset
1.1. message queue是无限长的数组，一条数据进来下标就会涨1，下标就是offset,
    消息在某个MessageQueue里的位置，通过offset可以定位到这条消息，或者指示
    Consumer从这条消息开始往后处理
1.2. message queue中的maxOffset表示消息的最大offset, maxOffset将不是最新的那条消息的 offset，而是最新消息的offset+1，minOffset则是现存在的最小offset.
     fileReserveTime=48默认消息存储48小时后，消费会被物理地从磁盘删除，message queue 的min offset也就对应增长.
     所以比minOffset还要小的那些消息已经不在broker上了，就无法被消费
2.类型(父类是OffsetStore): 
    2.1.本地文件类型 
    DefaultMQPushConsumer 的 BROADCASTING 模式，各个 Consumer 没有互相干扰，使用LoclaFileOffsetStore,把Offset存储在本地.
    2.2.Broker存储类型
    DefaultMQPushConsumer 的 CLUSTERING 模式，由 Broker 端存储和控制 Offset 的值，使用 RemoteBrokerOffsetStore
3.有什么用
    3.1.主要是记录消息的偏移量，有多个消费者进行消费.
    3.2.集群模式下采用RemoteBrokerOffsetStore，broker控制offset的值 
    3.3.广播模式下采用LocaFileOffsetStore，消费端存储
4.建议采用pushConsumer，RocketMQ自动维护OffsetStore，如果用另外一种pullConsumer需要自己进行维护OffsetStore
`