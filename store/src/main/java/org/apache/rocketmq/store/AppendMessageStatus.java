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
package org.apache.rocketmq.store;

/**
 * When write a message to the commit log, returns code
 */
/**
 * 向 commit log 写消息的返回码
 */
public enum AppendMessageStatus {
    PUT_OK,  // 成功 Append
    END_OF_FILE,  // 当前 MapperFile 放不下这条消息，新建一个 MapperFile
    MESSAGE_SIZE_EXCEEDED,  // 消息大小超过限制
    PROPERTIES_SIZE_EXCEEDED,  // 消息的 property 大小超出限制
    UNKNOWN_ERROR,  // 不知道发生了什么，反正就是失败了
}
