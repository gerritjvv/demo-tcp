(ns demo-tcp.client
    (:require [clojure.tools.logging :refer [error info]])
    (:import (java.net Socket SocketException)
      (java.io InputStream OutputStream BufferedInputStream BufferedOutputStream DataInputStream)
      (io.netty.buffer ByteBuf)))


;; Simple Direct TCP Client for the producer
;; The producers sit behind an async buffer where data is pushed on
;; by multiple threads, the TCP sending itself does not need yet another layer or indirection
;; which at the current moment under high loads with huge messages can cause out of memory errors

;;; Usage
;;; (def client (tcp/tcp-client "localhost" 7002))
;;; (tcp/write! client "one two three" :flush true)
;;; (tcp/read-async-loop! client (fn [^"[B" bts] (prn (String. bts))))
;;; (tcp/close! client)

(defrecord TCPClient [host port conf socket ^BufferedInputStream input ^BufferedOutputStream output])

(defprotocol TCPWritable (-write! [obj tcp-client] "Write obj to the tcp client"))

(defn tcp-client
      "Creates a tcp client from host port and conf
       InputStream is DataInputStream(BufferedInputStream) and output is BufferedOutputStream"
      [host port & conf]
      {:pre [(string? host) (number? port)]}
      (let [socket (Socket. (str host) (int port))]
           (.setSendBufferSize socket (int (* 1048576 2)))
           (.setReceiveBufferSize socket (int (* 1048576 2)))

           (->TCPClient host port conf socket
                        (DataInputStream. (BufferedInputStream. (.getInputStream socket)))
                        (BufferedOutputStream. (.getOutputStream socket)))))

(defn read-async-loop!
      "Only call this once on the tcp-client, it will create a background thread that exits when the socket is closed.
       The message must always be [4 bytes size N][N bytes]"
      [{:keys [^Socket socket ^DataInputStream input]} handler]
      {:pre [socket input (fn? handler)]}
      (future
        (try
          (while true
                 (let [size (.readInt input)
                       bts (byte-array size)
                       _ (.read input bts)]
                      (prn "GOT data")
                      (try
                        (handler bts)
                        (catch SocketException e nil)
                        (catch Exception e (prn e e)))))
          (catch SocketException e (prn "Socket error " e)))
        (prn "Exit read loop")))

(defn write! [tcp-client obj & {:keys [flush] :or {flush false}}]
      (when obj
            (-write! obj tcp-client)
            (if flush
              (.flush ^BufferedOutputStream (:output tcp-client)))))

(defn close! [{:keys [^Socket socket ^InputStream input ^OutputStream output]}]
      {:pre [socket]}
      (info "tcp/close!: socket: " (.isClosed socket))
      (when (not (.isClosed socket))
            (.flush output)
            (.close output)
            (.close input)
            (.close socket)))

(defn- _write-bytes [tcp-client ^"[B" bts]
       (.write ^BufferedOutputStream (:output tcp-client) bts))

(extend-protocol TCPWritable
                 (Class/forName "[B")
                 (-write! [obj tcp-client]
                          (_write-bytes tcp-client obj))
                 ByteBuf
                 (-write! [obj tcp-client]
                          (let [^ByteBuf buff obj
                                readable-bytes (.readableBytes buff)]
                               (.readBytes buff ^OutputStream (:output tcp-client) (int readable-bytes))))
                 String
                 (-write! [obj tcp-client]
                          (_write-bytes tcp-client (.getBytes ^String obj "UTF-8"))))