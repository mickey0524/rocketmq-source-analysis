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

package org.apache.rocketmq.logging.inner;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


public class Logger implements Appender.AppenderPipeline {

    private static final String FQCN = Logger.class.getName();

    private static final DefaultLoggerRepository REPOSITORY = new DefaultLoggerRepository(new RootLogger(Level.DEBUG));

    public static LoggerRepository getRepository() {
        return REPOSITORY;
    }

    private String name;

    volatile private Level level;

    volatile private Logger parent;  // 父亲 Logger 节点

    Appender.AppenderPipelineImpl appenderPipeline;

    private boolean additive = true;  // Logger 不可向上加

    private Logger(String name) {
        this.name = name;
    }

    static public Logger getLogger(String name) {
        return getRepository().getLogger(name);
    }

    static public Logger getLogger(Class clazz) {
        return getRepository().getLogger(clazz.getName());
    }

    public static Logger getRootLogger() {
        return getRepository().getRootLogger();
    }

    synchronized public void addAppender(Appender newAppender) {
        if (appenderPipeline == null) {
            appenderPipeline = new Appender.AppenderPipelineImpl();
        }
        appenderPipeline.addAppender(newAppender);
    }

    public void callAppenders(LoggingEvent event) {
        int writes = 0;

        for (Logger logger = this; logger != null; logger = logger.parent) {
            synchronized (logger) {
                if (logger.appenderPipeline != null) {
                    writes += logger.appenderPipeline.appendLoopOnAppenders(event);
                }
                if (!logger.additive) {
                    break;
                }
            }
        }

        if (writes == 0) {
            getRepository().emitNoAppenderWarning(this);
        }
    }

    synchronized void closeNestedAppenders() {
        Enumeration enumeration = this.getAllAppenders();
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                Appender a = (Appender) enumeration.nextElement();
                if (a instanceof Appender.AppenderPipeline) {
                    a.close();
                }
            }
        }
    }

    public void debug(Object message) {
        if (getRepository().isDisabled(Level.DEBUG_INT)) {
            return;
        }
        if (Level.DEBUG.isGreaterOrEqual(this.getEffectiveLevel())) {
            forcedLog(FQCN, Level.DEBUG, message, null);
        }
    }


    public void debug(Object message, Throwable t) {
        if (getRepository().isDisabled(Level.DEBUG_INT)) {
            return;
        }
        if (Level.DEBUG.isGreaterOrEqual(this.getEffectiveLevel())) {
            forcedLog(FQCN, Level.DEBUG, message, t);
        }
    }


    public void error(Object message) {
        if (getRepository().isDisabled(Level.ERROR_INT)) {
            return;
        }
        if (Level.ERROR.isGreaterOrEqual(this.getEffectiveLevel())) {
            forcedLog(FQCN, Level.ERROR, message, null);
        }
    }

    public void error(Object message, Throwable t) {
        if (getRepository().isDisabled(Level.ERROR_INT)) {
            return;
        }
        if (Level.ERROR.isGreaterOrEqual(this.getEffectiveLevel())) {
            forcedLog(FQCN, Level.ERROR, message, t);
        }

    }

    // 触发打印 log
    protected void forcedLog(String fqcn, Level level, Object message, Throwable t) {
        callAppenders(new LoggingEvent(fqcn, this, level, message, t));
    }


    synchronized public Enumeration getAllAppenders() {
        if (appenderPipeline == null) {
            return null;
        } else {
            return appenderPipeline.getAllAppenders();
        }
    }

    synchronized public Appender getAppender(String name) {
        if (appenderPipeline == null || name == null) {
            return null;
        }

        return appenderPipeline.getAppender(name);
    }

    // 获取 Logger 的 Log 级别
    public Level getEffectiveLevel() {
        for (Logger c = this; c != null; c = c.parent) {
            if (c.level != null) {
                return c.level;
            }
        }
        return null;
    }

    public final String getName() {
        return name;
    }

    final public Level getLevel() {
        return this.level;
    }


    public void info(Object message) {
        if (getRepository().isDisabled(Level.INFO_INT)) {
            return;
        }
        if (Level.INFO.isGreaterOrEqual(this.getEffectiveLevel())) {
            forcedLog(FQCN, Level.INFO, message, null);
        }
    }

    public void info(Object message, Throwable t) {
        if (getRepository().isDisabled(Level.INFO_INT)) {
            return;
        }
        if (Level.INFO.isGreaterOrEqual(this.getEffectiveLevel())) {
            forcedLog(FQCN, Level.INFO, message, t);
        }
    }

    public boolean isAttached(Appender appender) {
        return appender != null && appenderPipeline != null && appenderPipeline.isAttached(appender);
    }

    synchronized public void removeAllAppenders() {
        if (appenderPipeline != null) {
            appenderPipeline.removeAllAppenders();
            appenderPipeline = null;
        }
    }

    synchronized public void removeAppender(Appender appender) {
        if (appender == null || appenderPipeline == null) {
            return;
        }
        appenderPipeline.removeAppender(appender);
    }

    synchronized public void removeAppender(String name) {
        if (name == null || appenderPipeline == null) {
            return;
        }
        appenderPipeline.removeAppender(name);
    }

    public void setAdditivity(boolean additive) {
        this.additive = additive;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void warn(Object message) {
        if (getRepository().isDisabled(Level.WARN_INT)) {
            return;
        }

        if (Level.WARN.isGreaterOrEqual(this.getEffectiveLevel())) {
            forcedLog(FQCN, Level.WARN, message, null);
        }
    }

    public void warn(Object message, Throwable t) {
        // 不到 DefaultLoggerRepository 的 Log 级别
        if (getRepository().isDisabled(Level.WARN_INT)) {
            return;
        }
        // 满足 Logger 的 Log 级别
        if (Level.WARN.isGreaterOrEqual(this.getEffectiveLevel())) {
            forcedLog(FQCN, Level.WARN, message, t);
        }
    }

    public interface LoggerRepository {

        boolean isDisabled(int level);

        void setLogLevel(Level level);

        void emitNoAppenderWarning(Logger cat);

        Level getLogLevel();

        Logger getLogger(String name);

        Logger getRootLogger();

        Logger exists(String name);

        void shutdown();

        Enumeration getCurrentLoggers();
    }

    // 一个 Vector，存储多个子 Logger（当父亲节点不存在的时候缓存住孩子节点的 Logger）
    public static class ProvisionNode extends Vector<Logger> {

        ProvisionNode(Logger logger) {
            super();
            addElement(logger);
        }
    }

    // 默认的 Logger 仓库，使用 FlyWeight（享元）设计模式，用 HashMap 存储
    public static class DefaultLoggerRepository implements LoggerRepository {

        // 这里不知道为啥要用 Hashtable
        final Hashtable<CategoryKey,Object> ht = new Hashtable<CategoryKey,Object>();
        Logger root;

        // LogLevel 以及 LogLevel 的 int 数值
        int logLevelInt;
        Level logLevel;

        // 当没有正确初始化的时候，Warning 一次
        boolean emittedNoAppenderWarning = false;

        public DefaultLoggerRepository(Logger root) {
            this.root = root;
            setLogLevel(Level.ALL);
        }

        public void emitNoAppenderWarning(Logger cat) {
            if (!this.emittedNoAppenderWarning) {
                // 没有 appenders
                SysLogger.warn("No appenders could be found for logger (" + cat.getName() + ").");
                SysLogger.warn("Please initialize the logger system properly.");
                this.emittedNoAppenderWarning = true;
            }
        }

        // 是否存在 name 这个命名空间对应的 Logger，存在返回 Logger，不存在返回 null
        public Logger exists(String name) {
            Object o = ht.get(new CategoryKey(name));
            if (o instanceof Logger) {
                return (Logger) o;
            } else {
                return null;
            }
        }

        // 设置 LogLevel
        public void setLogLevel(Level l) {
            if (l != null) {
                logLevelInt = l.level;
                logLevel = l;
            }
        }

        public Level getLogLevel() {
            return logLevel;
        }

        // 根据命名空间获取 Logger
        public Logger getLogger(String name) {
            CategoryKey key = new CategoryKey(name);
            Logger logger;

            synchronized (ht) {
                Object o = ht.get(key);
                if (o == null) {
                    // 创建一个新的 Logger 实栗
                    logger = makeNewLoggerInstance(name);
                    ht.put(key, logger);
                    updateParents(logger);
                    return logger;
                } else if (o instanceof Logger) {
                    // 如果 Object 是 Logger 类型，直接返回
                    return (Logger) o;
                } else if (o instanceof ProvisionNode) {
                    logger = makeNewLoggerInstance(name);
                    ht.put(key, logger);
                    updateChildren((ProvisionNode) o, logger);
                    updateParents(logger);
                    return logger;
                } else {
                    return null;
                }
            }
        }

        public Logger makeNewLoggerInstance(String name) {
            return new Logger(name);
        }

        // 将 ht 中的 Logger 实栗找出来（需要排除 ProvisionNode 实栗）
        public Enumeration getCurrentLoggers() {
            Vector<Logger> loggers = new Vector<Logger>(ht.size());

            Enumeration elems = ht.elements();
            while (elems.hasMoreElements()) {
                Object o = elems.nextElement();
                if (o instanceof Logger) {
                    Logger logger = (Logger)o;
                    loggers.addElement(logger);
                }
            }
            return loggers.elements();
        }

        // 获取 root Logger
        public Logger getRootLogger() {
            return root;
        }

        // Log 级别不够的 disable
        public boolean isDisabled(int level) {
            return logLevelInt > level;
        }


        public void shutdown() {
            Logger root = getRootLogger();
            root.closeNestedAppenders();

            synchronized (ht) {
                Enumeration cats = this.getCurrentLoggers();
                while (cats.hasMoreElements()) {
                    Logger c = (Logger) cats.nextElement();
                    c.closeNestedAppenders();
                }
                root.removeAllAppenders();
            }
        }

        // 判断 Logger 的 parent 是哪个 Logger
        private void updateParents(Logger cat) {
            String name = cat.name;
            int length = name.length();
            boolean parentFound = false;

            // A.B.C 这样的名字
            for (int i = name.lastIndexOf('.', length - 1); i >= 0;
                 i = name.lastIndexOf('.', i - 1)) {
                String substr = name.substring(0, i);

                CategoryKey key = new CategoryKey(substr);
                Object o = ht.get(key);
                if (o == null) {
                    // 创建一个虚假的 Logger 节点，存储孩子 Logger
                    ht.put(key, new ProvisionNode(cat));
                } else if (o instanceof Logger) {
                    // 找到了真实的 Logger
                    parentFound = true;
                    cat.parent = (Logger) o;
                    break;
                } else if (o instanceof ProvisionNode) {
                    ((ProvisionNode) o).addElement(cat);
                } else {
                    Exception e = new IllegalStateException("unexpected object type " + o.getClass() + " in ht.");
                    e.printStackTrace();
                }
            }
            if (!parentFound) {
                cat.parent = root;
            }
        }

        // 更新之前缓存的孩子节点的父节点
        private void updateChildren(ProvisionNode pn, Logger logger) {
            final int last = pn.size();

            for (int i = 0; i < last; i++) {
                Logger l = pn.elementAt(i);
                // 当 logger 比之前子节点的 parent 更靠近子节点的时候，更新
                if (!l.parent.name.startsWith(logger.name)) {
                    logger.parent = l.parent;
                    l.parent = logger;
                }
            }
        }

        // 日志分类的 key，例如 RocketmqNamesrv，放置于
        // org.apache.rocketmq.common.constant.LoggerName 中
        private class CategoryKey {

            String name;
            int hashCache;

            CategoryKey(String name) {
                this.name = name;
                hashCache = name.hashCode();
            }

            final public int hashCode() {
                return hashCache;
            }

            final public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }

                if (o != null && o instanceof CategoryKey) {
                    CategoryKey cc = (CategoryKey) o;
                    return name.equals(cc.name);
                } else {
                    return false;
                }
            }
        }

    }

    // 根 Logger，Level 是 Debug，其实就是 ALL
    public static class RootLogger extends Logger {

        public RootLogger(Level level) {
            super("root");
            setLevel(level);
        }
    }
}
