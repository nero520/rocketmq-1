https://www.jianshu.com/p/2ce8aa1bd438
RocketMQ操作CommitLog, ConsumeQueue文件, 都是基于内存映射方法并在启动的时候, 会加载commitlog, ConsumeQueue目录下的所有文件,
为了避免内存与磁盘的浪费, 不可能将消息永久存储在消息服务器上, 所以需要一种机制来删除已过期的文件. 
RocketMQ顺序写Commitlog、ConsumeQueue文件, 所有写操作全部落在最后一个CommitLog或ConsumeQueue文件上, 之前的文件在下一个文件创建后, 将不会再被更新,
RocketMQ清除过期文件的方法是: 
如果非当前写文件在一定时间间隔内没有再次被更新, 则认为是过期文件, 可以被删除, 
RocketMQ不会管这个这个文件上的消息是否被全部消费. 默认每个文件的过期时间为72小时. 
通过在Broker配置文件中设置fileReservedTime来改变过期时间, 单位为小时
