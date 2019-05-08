1.线程池ConsumeMessageThread_
线程池名称:ConsumeMessageThread_
使用的jdk线程:new ThreadPoolExecutor() 
线程池核心池的大小（corePoolSize）:20(对应consumeThreadMin) 
线程池最大线程数（maximumPoolSize）:64（对应consumeThreadMax）
线程池拒绝处理任务时的策略:AbortPolicy:丢弃任务并抛出RejectedExecutionException异常。
核心线程设置存活时间（keepAliveTime）:1000 * 60（毫秒）
线程池作用:
1.