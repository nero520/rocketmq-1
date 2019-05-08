在讲broker收发消息前，先看一下topic的创建过程。
topic的创建有两种方式：
一种是broker支持在收发消息时自动创建，比如producer发过来的消息带了一个不存在的topic，如果broker设置成可自动创建的话，会自动尝试创建topic。
另外一种就是通过管理接口创建，这种方式生产环境用的更多一些，因为可以由管理员来统一管理topic。
客户端
RocketMQ提供了管理接口MQAdmin来支持用户的后台管理需求，比如topic创建，消息查询等。默认实现方法是MQAdminImpl.createTopic():
public void createTopic(String key, String newTopic, int queueNum, int topicSysFlag) throws MQClientException {
        try {
            //1、一般使用defaultTopic获取已经存在的broker data，所有的broker默认都支持defaultTopic
            TopicRouteData topicRouteData = this.mQClientFactory.getMQClientAPIImpl().getTopicRouteInfoFromNameServer(key, timeoutMillis);
            List<BrokerData> brokerDataList = topicRouteData.getBrokerDatas();
            if (brokerDataList != null && !brokerDataList.isEmpty()) {
                Collections.sort(brokerDataList);

                boolean createOKAtLeastOnce = false;
                MQClientException exception = null;

                StringBuilder orderTopicString = new StringBuilder(); //没用到
                //2、轮询所有broker，在master上创建topic，中间有一个broker失败，则中止创建
                for (BrokerData brokerData : brokerDataList) {
                    String addr = brokerData.getBrokerAddrs().get(MixAll.MASTER_ID);
                    if (addr != null) {
                        TopicConfig topicConfig = new TopicConfig(newTopic);
                        //3、设置queue的数量
                        topicConfig.setReadQueueNums(queueNum);
                        topicConfig.setWriteQueueNums(queueNum);
                        
                        topicConfig.setTopicSysFlag(topicSysFlag);

                        boolean createOK = false;
                        for (int i = 0; i < 5; i++) {//重试4次
                            try {
                                this.mQClientFactory.getMQClientAPIImpl().createTopic(addr, key, topicConfig, timeoutMillis);
                                createOK = true;
                                createOKAtLeastOnce = true;
                                break;
                            } catch (Exception e) {
                                if (4 == i) {
                                    exception = new MQClientException("create topic to broker exception", e);
                                }
                            }
                        }

                        if (createOK) {
                            orderTopicString.append(brokerData.getBrokerName());
                            orderTopicString.append(":");
                            orderTopicString.append(queueNum);
                            orderTopicString.append(";");
                        }
                    }
                }

                if (exception != null && !createOKAtLeastOnce) {
                    throw exception;
                }
            } else {
                throw new MQClientException("Not found broker, maybe key is wrong", null);
            }
        } catch (Exception e) {
            throw new MQClientException("create new topic failed", e);
        }
    }

这个方法接收4个参数：
key：这个参数是系统已经存在的一个topic的名称，新建的topic会跟它在相同的broker上创建
newTopic：新建的topic的唯一标识
queueNum：指定topic中queue的数量
topicSysFlag：topic的标记位设置，没有特殊要求就填0就可以了。可选值在TopicSysFlag中定义

第1步，根据提供的key代表的topic去获取broker的路由，如果想在所有broker创建，一般使用DefaultTopic，因为这个topic是在所有broker上都存在的。
第2步，轮询所有的broker，在master上创建topic，中间有一个broker失败，则中止创建，返回失败。因为master和slave的配置数据也会自动同步，所以只需要在master上创建。
第3，4步，设置参数
第5步，调用MQClientAPIImpl接口创建，失败会重试4次。

上面第5步中调用的接口实现如下：
public void createTopic(final String addr, final String defaultTopic, final TopicConfig topicConfig,
        final long timeoutMillis)
        throws RemotingException, MQBrokerException, InterruptedException, MQClientException {
        CreateTopicRequestHeader requestHeader = new CreateTopicRequestHeader();
        requestHeader.setTopic(topicConfig.getTopicName());
        requestHeader.setDefaultTopic(defaultTopic);
        requestHeader.setReadQueueNums(topicConfig.getReadQueueNums());
        requestHeader.setWriteQueueNums(topicConfig.getWriteQueueNums());
        //设置topic的权限，可读，可写
        requestHeader.setPerm(topicConfig.getPerm());
        //设置topic支持的消息过滤类型
        requestHeader.setTopicFilterType(topicConfig.getTopicFilterType().name());
        requestHeader.setTopicSysFlag(topicConfig.getTopicSysFlag());
        //设置是否是顺序消息topic
        requestHeader.setOrder(topicConfig.isOrder());

        RemotingCommand request = RemotingCommand.createRequestCommand(RequestCode.UPDATE_AND_CREATE_TOPIC, requestHeader);

        RemotingCommand response = this.remotingClient.invokeSync(MixAll.brokerVIPChannel(this.clientConfig.isVipChannelEnabled(), addr),
            request, timeoutMillis);
        assert response != null;
        switch (response.getCode()) {
            case ResponseCode.SUCCESS: {
                return;
            }
            default:
                break;
        }

        throw new MQClientException(response.getCode(), response.getRemark());
    }

该方法其实就是封装成命令(RequestCode.UPDATE_AND_CREATE_TOPIC)发给broker，可以看到topic创建还支持其它参数的设置。
Broker创建topic
Client将创建的命令发出后，在Broker上通过AdminBrokerProcessor处理所有的管理请求的。我们看下updateAndCreateTopic()方法的实现
private synchronized RemotingCommand updateAndCreateTopic(ChannelHandlerContext ctx,
        RemotingCommand request) throws RemotingCommandException {
        final RemotingCommand response = RemotingCommand.createResponseCommand(null);
        final CreateTopicRequestHeader requestHeader =
            (CreateTopicRequestHeader) request.decodeCommandCustomHeader(CreateTopicRequestHeader.class);
        log.info("updateAndCreateTopic called by {}", RemotingHelper.parseChannelRemoteAddr(ctx.channel()));
        //1、判断topicName的合法性，不能和clusterName同名
        if (requestHeader.getTopic().equals(this.brokerController.getBrokerConfig().getBrokerClusterName())) {
            String errorMsg = "the topic[" + requestHeader.getTopic() + "] is conflict with system reserved words.";
            log.warn(errorMsg);
            response.setCode(ResponseCode.SYSTEM_ERROR);
            response.setRemark(errorMsg);
            return response;
        }

        try {//2、先回复客户端创建成功，后更新broker缓存
            response.setCode(ResponseCode.SUCCESS);
            response.setOpaque(request.getOpaque());
            response.markResponseType();
            response.setRemark(null);
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            log.error("Failed to produce a proper response", e);
        }

        TopicConfig topicConfig = new TopicConfig(requestHeader.getTopic());
        topicConfig.setReadQueueNums(requestHeader.getReadQueueNums());
        topicConfig.setWriteQueueNums(requestHeader.getWriteQueueNums());
        topicConfig.setTopicFilterType(requestHeader.getTopicFilterTypeEnum());
        topicConfig.setPerm(requestHeader.getPerm());
        topicConfig.setTopicSysFlag(requestHeader.getTopicSysFlag() == null ? 0 : requestHeader.getTopicSysFlag());
        //3、更新TopicConfigManager中的topic配置信息。不存在则创建，存在则更新，并且持久化到文件中
        this.brokerController.getTopicConfigManager().updateTopicConfig(topicConfig);
        //4、broker将topic信息同步到nameserv
        this.brokerController.registerIncrementBrokerData(topicConfig,this.brokerController.getTopicConfigManager().getDataVersion());
        return null;
    }

以上的代码实现可以看到，在broker收到命令后，参数检查通过就直接返回成功了，这个逻辑不是非常理解。在将topic保存后，broker会将新增的topic同步给NameServer，同步的过程跟broker注册是一样的。
总结
Broker的topic创建是通过Client调用管理接口实现的。NameServer的topic配置是通过broker上报的，是单向同步。所以在broker宕机期间topic如果发生变化，只能通过Client重新更新才能将变化同步给Broker。

