package com.alibaba.mesh.remoting.exchange;

import com.alibaba.mesh.common.Constants;
import com.alibaba.mesh.common.URL;
import com.alibaba.mesh.common.Version;
import com.alibaba.mesh.common.extension.ExtensionLoader;
import com.alibaba.mesh.remoting.RemotingException;

/**
 * Exchanger facade. (API, Static, ThreadSafe)
 */
public class Exchangers {

    static {
        // check duplicate jar package
        Version.checkDuplicate(Exchangers.class);
    }

    private Exchangers() {
    }

    public static ExchangeServer bind(String url, ExchangeHandler handler) throws RemotingException {
        return bind(URL.valueOf(url), handler);
    }

    public static ExchangeServer bind(URL url, ExchangeHandler handler) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");
        return getExchanger(url).bind(url, handler);
    }

//    public static ExchangeClient connect(String url) throws RemotingException {
//        return connect(URL.valueOf(url));
//    }

//    public static ExchangeClient connect(URL url) throws RemotingException {
//        return connect(url, new ChannelHandlerAdapter(), null);
//    }
//
//    public static ExchangeClient connect(String url, Replier<?> replier) throws RemotingException {
//        return connect(URL.valueOf(url), new ChannelHandlerAdapter(), replier);
//    }
//
//    public static ExchangeClient connect(URL url, Replier<?> replier) throws RemotingException {
//        return connect(url, new ChannelHandlerAdapter(), replier);
//    }
//
//    public static ExchangeClient connect(String url, ChannelHandler handler, Replier<?> replier) throws RemotingException {
//        return connect(URL.valueOf(url), handler, replier);
//    }
//
//    public static ExchangeClient connect(URL url, ChannelHandler handler, Replier<?> replier) throws RemotingException {
//        return connect(url, new ExchangeHandlerDispatcher(replier, handler));
//    }

    public static ExchangeClient connect(String url, ExchangeHandler handler) throws RemotingException {
        return connect(URL.valueOf(url), handler);
    }

    public static ExchangeClient connect(URL url, ExchangeHandler handler) throws RemotingException {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        url = url.addParameterIfAbsent(Constants.CODEC_KEY, "exchange");
        return getExchanger(url).connect(url, handler);
    }

    public static Exchanger getExchanger(URL url) {
        String type = url.getParameter(Constants.EXCHANGER_KEY, Constants.DEFAULT_EXCHANGER);
        return getExchanger(type);
    }

    public static Exchanger getExchanger(String type) {
        return ExtensionLoader.getExtensionLoader(Exchanger.class).getExtension(type);
    }

}