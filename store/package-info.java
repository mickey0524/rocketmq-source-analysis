// store 是 broker 中用于存储数据的组件
// 主要由 CommitLog 和 ConsumeQueue 组成
// CommitLog 存储真正的消息实体，ConsumeQueue 存储消息的索引（消息的物理位移和实体大小）
//
// 当消费消息的时候，首先根据消费位点获取对应的 ConsumeQueue 的偏移，然后取出物理位移和大小
// 进而去 CommitLog 中取出消息
//
// 当写消息的时候，直接就写入 CommitLog 了（最终写入 MappedFile）
// MappedFile 中使用 FileChannle 和 MappedByteBuffer/ByteBuffer 来存储数据
// CommitLog 中有两个线程，复制定时 commit（从 ByteBuffer -> FileChannel）/flush（FileChannel/MappedByteBuffer -> disk） 数据
// MessageStore 中有线程定时读取 CommitLog 中的数据，生成 Dispatcher 写入 ConsumeQueue
// MessageStore 中还有两个线程负责删除 CommitLog 和 ConsumeQueue 中的无效 MappedFile
// 根据 minPhyOffset 可以删除 ConsumeQueue，根据时间，磁盘使用量，删除次数可以删除 Commit
package org.apache.rocketmq.store;