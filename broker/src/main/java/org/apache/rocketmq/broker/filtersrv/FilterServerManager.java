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

package org.apache.rocketmq.broker.filtersrv;

import io.netty.channel.Channel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.broker.BrokerController;
import org.apache.rocketmq.broker.BrokerStartup;
import org.apache.rocketmq.common.ThreadFactoryImpl;
import org.apache.rocketmq.common.constant.LoggerName;
import org.apache.rocketmq.logging.InternalLogger;
import org.apache.rocketmq.logging.InternalLoggerFactory;
import org.apache.rocketmq.remoting.common.RemotingUtil;

// rocketmq 默认为客户端自己进行消息过滤，也可以使用 server filter
// broker 所在的机器会启动多个 FilterServer 过滤线程
// Consumer 启动后，会向 FilterServer 上传一个过滤的 Java 类
// Consumer 从 FilterServer 拉消息，FilterServer 将请求转収给 Broker，FilterServer 从 Broker 收到消息后，挄照
// Consumer 上传的 Java 过滤程序做过滤，过滤完成后返回给 Consumer
// FilterServerManager 来管理服务端消息过滤
public class FilterServerManager {

    public static final long FILTER_SERVER_MAX_IDLE_TIME_MILLS = 30000;  // 最多空闲 30s
    private static final InternalLogger log = InternalLoggerFactory.getLogger(LoggerName.BROKER_LOGGER_NAME);
    private final ConcurrentMap<Channel, FilterServerInfo> filterServerTable =
        new ConcurrentHashMap<Channel, FilterServerInfo>(16);
    private final BrokerController brokerController;

    private ScheduledExecutorService scheduledExecutorService = Executors
        .newSingleThreadScheduledExecutor(new ThreadFactoryImpl("FilterServerManagerScheduledThread"));

    public FilterServerManager(final BrokerController brokerController) {
        this.brokerController = brokerController;
    }

    public void start() {

        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    FilterServerManager.this.createFilterServer();
                } catch (Exception e) {
                    log.error("", e);
                }
            }
        }, 1000 * 5, 1000 * 30, TimeUnit.MILLISECONDS);  // 5s 后执行 createFilterServer 方法，然后没 30s 定时执行一次
    }

    public void createFilterServer() {
        // more 代表还需要启动的 filterServer 线程
        int more =
            this.brokerController.getBrokerConfig().getFilterServerNums() - this.filterServerTable.size();
        String cmd = this.buildStartCommand();
        for (int i = 0; i < more; i++) {
            FilterServerUtil.callShell(cmd, log);
        }
    }

    // build shell 命令
    private String buildStartCommand() {
        // 这里为啥不用 StringBuilder...虽然 JVM 会自动优化，但是代码洁癖啊--
        String config = "";
        if (BrokerStartup.configFile != null) {
            config = String.format("-c %s", BrokerStartup.configFile);
        }

        if (this.brokerController.getBrokerConfig().getNamesrvAddr() != null) {
            config += String.format(" -n %s", this.brokerController.getBrokerConfig().getNamesrvAddr());
        }
        
        // 分平台，获得 shell cmd
        if (RemotingUtil.isWindowsPlatform()) {
            return String.format("start /b %s\\bin\\mqfiltersrv.exe %s",
                this.brokerController.getBrokerConfig().getRocketmqHome(),
                config);
        } else {
            return String.format("sh %s/bin/startfsrv.sh %s",
                this.brokerController.getBrokerConfig().getRocketmqHome(),
                config);
        }
    }

    public void shutdown() {
        this.scheduledExecutorService.shutdown();
    }

    // 注册 filterServer
    public void registerFilterServer(final Channel channel, final String filterServerAddr) {
        FilterServerInfo filterServerInfo = this.filterServerTable.get(channel);
        // 如果 channel 已经对应一个 filterServer，更新 ts
        if (filterServerInfo != null) {
            filterServerInfo.setLastUpdateTimestamp(System.currentTimeMillis());
        } else {
            filterServerInfo = new FilterServerInfo();
            filterServerInfo.setFilterServerAddr(filterServerAddr);
            filterServerInfo.setLastUpdateTimestamp(System.currentTimeMillis());
            this.filterServerTable.put(channel, filterServerInfo);
            log.info("Receive a New Filter Server<{}>", filterServerAddr);
        }
    }

    // 检查不活跃的 channel
    public void scanNotActiveChannel() {

        Iterator<Entry<Channel, FilterServerInfo>> it = this.filterServerTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Channel, FilterServerInfo> next = it.next();
            long timestamp = next.getValue().getLastUpdateTimestamp();
            Channel channel = next.getKey();
            // 30s 不活跃，删除 filterServer，关闭 channel
            if ((System.currentTimeMillis() - timestamp) > FILTER_SERVER_MAX_IDLE_TIME_MILLS) {
                log.info("The Filter Server<{}> expired, remove it", next.getKey());
                it.remove();
                RemotingUtil.closeChannel(channel);
            }
        }
    }

    // 删除 channel
    public void doChannelCloseEvent(final String remoteAddr, final Channel channel) {
        FilterServerInfo old = this.filterServerTable.remove(channel);
        if (old != null) {
            log.warn("The Filter Server<{}> connection<{}> closed, remove it", old.getFilterServerAddr(),
                remoteAddr);
        }
    }

    // 返回所有 filterServer 的地址
    public List<String> buildNewFilterServerList() {
        List<String> addr = new ArrayList<>();
        Iterator<Entry<Channel, FilterServerInfo>> it = this.filterServerTable.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Channel, FilterServerInfo> next = it.next();
            addr.add(next.getValue().getFilterServerAddr());
        }
        return addr;
    }

    // 静态内部类
    static class FilterServerInfo {
        private String filterServerAddr;  // filterServer 的地址
        private long lastUpdateTimestamp;  // 上次更新的时间戳

        public String getFilterServerAddr() {
            return filterServerAddr;
        }

        public void setFilterServerAddr(String filterServerAddr) {
            this.filterServerAddr = filterServerAddr;
        }

        public long getLastUpdateTimestamp() {
            return lastUpdateTimestamp;
        }

        public void setLastUpdateTimestamp(long lastUpdateTimestamp) {
            this.lastUpdateTimestamp = lastUpdateTimestamp;
        }
    }
}
