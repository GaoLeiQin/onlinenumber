package com.ledo.task;

import com.ledo.service.IAllServerInfoService;
import com.ledo.service.IOnlineNumberService;
import com.ledo.service.IUrlContentService;
import com.ledo.util.DateUtil;
import org.apache.log4j.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ledo.common.ThreadContant.*;

/**
 * 所有的自动更新任务
 * @author qgl
 * @date 2018/11/14
 */
public class AllTask {
    private Logger logger = Logger.getLogger(AllTask.class);
    private ScheduledThreadPoolExecutor scheduledExecutor = null;
    private static long delayTime = 0;
    private static AllTask instance = new AllTask();

    public static AllTask getInstance() {
        return instance;
    }

    /**
     * 开启自动更新任务（包括网页内容和添加服务器信息）
     * @param onlineNumberService
     */
    public void startAutoUpdateTask(IUrlContentService urlContentService, IOnlineNumberService onlineNumberService, IAllServerInfoService allServerInfoService) {
        scheduledExecutor = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE, new MyThreadFactory(NAME_PREVIOUS_UPDATE_DATA_POOL));
        delayTime = DateUtil.getWaitingLongTime(SAVE_SERVER_INFO_PERIOD);
        ScheduledExecutorService executorTaskThread = Executors.newSingleThreadScheduledExecutor(new MyThreadFactory(NAME_PREVIOUS_DELAY_EXECUTE_POOL));
        executorTaskThread.schedule(new DelayExeScheduleTask(scheduledExecutor, urlContentService, onlineNumberService, allServerInfoService), delayTime, TimeUnit.MILLISECONDS);
        logger.info("在 " + DateUtil.getRemainTime(delayTime) + " 后，将执行自动更新线程");
    }

    /**
     * 监视线程任务
     * @param onlineNumberService
     */
    public void executeMonitorThreadTask(IUrlContentService urlContentService, IOnlineNumberService onlineNumberService, IAllServerInfoService allServerInfoService) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new MyThreadFactory(NAME_PREVIOUS_MONITOR_POOL));
        MonitorThreadPoolTask monitorThreadPoolTask = new MonitorThreadPoolTask(this.scheduledExecutor, urlContentService, onlineNumberService, allServerInfoService);
        monitorThreadPoolTask.setThreadName(MONITOR_THREAD_NAME);
        long monitorDelayTime = MONITOR_THREAD_POOL_DELAY + delayTime;
        executorService.scheduleAtFixedRate(monitorThreadPoolTask, monitorDelayTime, MONITOR_THREAD_POOL_PERIOD, TimeUnit.MILLISECONDS);
        logger.info("监视线程在 "+ DateUtil.getRemainTime(monitorDelayTime) + " 后启动！");
    }

    /**
     * 自定义 ThreadFactory，可设置线程池名称
     */
    private class MyThreadFactory implements ThreadFactory {
        private final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        MyThreadFactory(String namePre) {
            SecurityManager scheduledExecutor = System.getSecurityManager();
            group = (scheduledExecutor != null) ? scheduledExecutor.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = namePre + " pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
