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
package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.exchange.MultiMessage;

import io.netty.channel.ChannelHandlerContext;

/**
 *
 * @see MultiMessage
 */
public class MultiMessageHandler extends AbstractChannelHandlerDelegate {

    public MultiMessageHandler(ChannelHandler handler) {
        super(handler);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {
        if (message instanceof MultiMessage) {
            MultiMessage list = (MultiMessage) message;
            for (Object obj : list) {
                handler.channelRead(ctx, obj);
            }
        } else {
            handler.channelRead(ctx, message);
        }
    }
}
