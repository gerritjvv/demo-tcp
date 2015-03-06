(ns demo-tcp.echo-server
    (:require [clojure.tools.logging :refer [info error]])
    (:import
      [java.net InetSocketAddress]
      [io.netty.buffer Unpooled ByteBuf]
      [io.netty.channel ChannelHandler ChannelInboundHandlerAdapter ChannelInitializer ChannelInitializer ChannelHandlerContext ChannelFutureListener]
      [io.netty.channel.nio NioEventLoopGroup]
      [io.netty.bootstrap ServerBootstrap]
      [io.netty.channel.socket.nio NioServerSocketChannel])
    )

(defrecord Server [group channel-future])


(defn echo-handler []
      (proxy [ChannelInboundHandlerAdapter]
             []
             (channelRead [^ChannelHandlerContext ctx msg]
                          (prn "Received : " msg " at server")
                          (let [buff (Unpooled/buffer)
                                len (.readableBytes ^ByteBuf msg)]
                               (.writeInt buff len)
                               (.writeBytes buff msg)
                               (.writeAndFlush ctx buff)))
             (channelReadComplete [^ChannelHandlerContext ctx]
                                  )
             (exceptionCaught [^ChannelHandlerContext ctx cause]
                              (error cause cause)
                              (.close ctx))))


(defn close-server [{:keys [group channel-future]}]
      (-> channel-future .channel .closeFuture)
      (.shutdownNow group))

(defn ^ChannelInitializer channel-initializer []
      (proxy [ChannelInitializer]
             []
             (initChannel [ch]
                          (-> ch (.pipeline) (.addLast (into-array ChannelHandler [(echo-handler)])))

                          )))

(defn start-server [port]
      (let [group (NioEventLoopGroup.)
            b (ServerBootstrap.)
            ]
           (-> b (.group group)
               (.channel NioServerSocketChannel)
               (.localAddress (InetSocketAddress. port))
               (.childHandler (channel-initializer))
               )
           (->Server group (-> b .bind .sync))))