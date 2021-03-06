package com.alibaba.mesh.remoting.transport;

import com.alibaba.mesh.remoting.ChannelHandler;
import com.alibaba.mesh.remoting.Decodeable;
import com.alibaba.mesh.remoting.RemotingException;
import com.alibaba.mesh.remoting.exchange.Request;
import com.alibaba.mesh.remoting.exchange.Response;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecodeHandlerSupport extends AbstractChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(DecodeHandlerSupport.class);

    public DecodeHandlerSupport(ChannelHandler handler) {
        super(handler);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws RemotingException {

        if (message instanceof Request) {
            decode(((Request) message).getData());
        }

        if (message instanceof Response) {
            decode(((Response) message).getResult());
        }

        if (message instanceof Decodeable) {
            decode(message);
        }

        handler.channelRead(ctx, message);
    }

    private void decode(Object message) {
        if (message != null && message instanceof Decodeable) {
            try {
                ((Decodeable) message).decode();
                if (log.isDebugEnabled()) {
                    log.debug("Decode decodeable message " + message.getClass().getName());
                }
            } catch (Throwable e) {
                if (log.isWarnEnabled()) {
                    log.warn("Call Decodeable.decode failed: " + e.getMessage(), e);
                }
            }
        }
    }

}
