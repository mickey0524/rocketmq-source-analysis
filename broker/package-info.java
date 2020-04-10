// broker 同 Producer 和 Consumer 交互，此为业务层面交互
// 同 store 交互，此为存储层面交互
// 同时存储各种配置文件，例如 topic 的配置、subscription 的配置
// 总而言之，言而总之，broker 在 RocketMQ 中起着中枢大脑的作用
// 可以称之为 “北通巫峡，南极潇湘”
package org.apache.rocketmq.broker;