// client 目录主要是服务于 Producer 和 Consumer 的
//
// Producer 负责向 Broker 投递消息
// 主要有三种投递方式 ——
// Oneway：投递出去就不管了，没有任何后续操作
// Sync：同步投递，Producer 需要同步等待投递消息的结果
// Async：异步投递，Producer 通过注册一个 SendCallBack 对象，当投递成功后，会调用其中的方法实现异步
// 在投递消息时，如果没有指定 MessageQueue，会选择一个 MQ 进行投递，当然也可以在 send 函数指定 MQ 进行投递
// 此外，还可以传入一个 MessageQueueSelector 对象，用于选择 MQ
// 在投递消息的时候，如果没找到对应 topic 的 queueData/brokerAddress，都会立即与 NameSrv 进行交互
//
// Consumer 负责从 Broker 消费消息
// 本包中提供了两种 Consumer ——
// PullConsumer 和 PushConsumer
// PullConsumer 是需要消费者调用 pull 方法，也就是说是消费者主动发起的消费动作
// Pull 类型的消费同样提供了 Async 和 Sync 两种方式，和 Producer 一样
// 异步的 Pull 类型需要提供一个 PullCallback 对象
// PushConsumer 依靠 PullMessageService 来调度消费
// PushConsumer 的 rebalance 操作会生成 PullRequest 写入 PullMessageService 的 Queue 中
// 然后 PullMessageService 取出 PullRequest 进行消费
// PushConsumer 又分为有序消费和并发消费，通过用户传入的 MessageListener 来判断
// 并发消费没有任何限制，ConsumeMessageConcurrentlyService 会调用
// MessageListenerConcurrently 来处理消息
// 有序消费针对每一个 MessageQueue，消费都是按照 queueOffset 顺序消费的
// 需要使用 ProcessQueue 来进行消息的暂存
// 对于并发消费来说，直接消费每次拉取到的 Message 列表就行
// 对于有序消费来说，需要将拉取到的 Message 列表存储 ProcessQueue 的 TreeMap 中
// TreeMap 以 queueOffset 作为 key，实现有序排序
// 有序消费会从 map 中 poll message 进行消费，因此需要 commit/rollback 操作（防止失败之后变为无序）
// 
// 消费者存在 rebalance 行为，RebalanceService 会定时触发所有 MQConsumer 的 rebalance 操作
// rebalance 操作就是在消费者或者 MQ 的数量改变的时候，重新为消费者分配需要消费的 MQ
// 消费者有两种消费方式，广播方式和集群方式，广播方式，consumerGroup 中的每一个消费者都需要消费
// 所有的 MQ，集群方式中，consumerGroup 中的消费者均摊全部的 MQ（同机房、均匀分配、一致性哈希等方法）

package org.apache.rocketmq.client;