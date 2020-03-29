// namesrv 用于集中存储 broker、topic 等相关信息，Producer 和 Consumer 都需要与 namesrc 进行交互
// 当 Producer 发送消息前，需要从 namesrv 中找到应该发往何处的 broker，同理，Consumer 需要知道从何处
// 消费，namesrv 简单来说可以分为两部分和其他配置文件，一个是 routerinfo，用于存放 cluster、broker、topic、queue
// 等相关信息，另外一个是 processor，用于作为 nettyServer 的处理类，接收 RemoteResquest，从 routerInfo 中
// 读写数据
package org.apache.rocketmq.namesrv;