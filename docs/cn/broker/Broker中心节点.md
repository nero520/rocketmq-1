1、Broker与NameSrv交互
Broker会不断发送register信息给NameSrv，将自身的clusterName，brokerName，topic信息发送到Namesrv；而Consumer和Producer则会通过Namesrv获取信息
2、Broker与Producer
Producer首先会从NameSrv获取所有的broker-topic-queue列表，然后向各个queue发送message，这里发送后的操作主要是在MessageStore中进行处理；

首先，将数据写入CommitLog；

其次，将信息写入DispatchMessageService，它将创建索引将信息写入ConsumerQueue和IndexService

最后，Producer会发送心跳信息，Broker会使用ProducerManager对Producer进行管理


3、Broker与Consumer
3.1 Consumer首先也会从NameSrv获取所有的broker-topic-queue列表；

3.2 Consumer获取各个broker上面的相同group-topic-broker的消费者信息

3.3其次，根据rebalance的规则，每个consumer消化broker的不同queue；

3.4再次，调用pullMessage获取消息进行消费

3.5最后，Consumer会发送心跳信息，Broker会使用ConsumerManager对Consumer进行管理


4、Schedule定时任务
Broker启动后，MQ会进行一系列的schedule服务，主要schedule如下：

4.1. messageStore：它的主要工作详见http://blog.csdn.net/xxxxxx91116/article/details/50333161

4.2. remotingServer：启动监听服务，在8888进行监听
4.3. brokerOuterAPI：发送的函数API
4.4. pullRequestHoldService：
4.5. clientHousekeepingService：管理consumer、producer的信息 
