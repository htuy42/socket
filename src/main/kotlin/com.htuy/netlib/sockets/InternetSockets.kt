package com.htuy.netlib.sockets

import com.htuy.common.Address
import com.htuy.common.Logging
import com.htuy.concurrent.CallbackQueue
import com.htuy.dependency.ModuleManager
import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.serialization.ClassResolvers
import io.netty.handler.codec.serialization.ObjectDecoder
import io.netty.handler.codec.serialization.ObjectEncoder
import kotlinx.coroutines.experimental.launch
import java.io.Serializable
import java.util.concurrent.LinkedBlockingQueue

class HandshakeMessage : Serializable

/**
 * Sockets, but with internet addresses and therefore support for networked apps
 */
class InternetSockets(private val moduleManager: ModuleManager? = null) : SocketProvider {

    private fun handleChannel(ch: SocketChannel, to: Address? = null): QueuedSocket {
        val queue1 = CallbackQueue(LinkedBlockingQueue<Message<out Serializable>>())
        val queue2 = CallbackQueue(LinkedBlockingQueue<Message<out Serializable>>())
        val socket = QueuedSocket(queue1,
                queue2,
                to ?: Address.fromInetSocketAddress(ch.remoteAddress()),
                moduleManager = moduleManager)
        socket.registerOnClose { ch.close() }
        ch.closeFuture().addListener {
            socket.close()
        }
        ch.pipeline()
                .addLast(ObjectDecoder(ClassResolvers.weakCachingResolver(null)),
                        ObjectEncoder())
                .addLast(object : ChannelInboundHandlerAdapter() {
                    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                        // if you put something else in the queue it will break it. But there's no real mechanism to do
                        // that except by using return channel.
                        msg as Message<out Serializable>
                        Logging.getLogger().trace { "Got message ${msg.contents} from ${Address.localHost} " }
                        if(msg is IdMessage<*>){
                            Logging.getLogger().trace{"Message id was ${msg.id}, and it is returning : ${msg.returning}"}
                        }
                        queue1.put(msg)
                    }
                })
                .addLast(object : ChannelInboundHandlerAdapter() {
                    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                        cause.printStackTrace()
                        ch.close()
                    }
                })
        queue2.registerCallback {
            if (ch.isOpen) {
                while (!queue2.isEmpty()) {
                    val fromQ = queue2.poll() ?: continue
                    if (fromQ.contents !is Serializable){
                        Logging.getLogger().error{"Tried to send non-serializable object ${fromQ.contents}"}
                        throw IllegalStateException("Trying to send non-serializable object!")
                    }
                    ch.pipeline().writeAndFlush(fromQ)
                    Logging.getLogger().trace{"Performing actual send of ${fromQ.contents}"}
                }
                Logging.getLogger().trace{"flushing channel!"}
                ch.flush()
            }
        }



        return socket
    }

    override fun getSocketTo(to: Address, from: Address): Socket {
        val workerGroupS = NioEventLoopGroup()
        val b = Bootstrap()
        var res: QueuedSocket? = null
        b.group(workerGroupS)
        b.channel(NioSocketChannel::class.java as Class<out Channel>?)
        b.option(ChannelOption.SO_KEEPALIVE, true)
        b.handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                res = handleChannel(ch, to)
            }
        })
        val chan = b.connect(to.address, to.port).sync().await().channel()
        launch {
            chan.closeFuture().sync()
            res?.close()
            workerGroupS.shutdownGracefully()
        }
        return res!!
    }

    override fun listenOn(self: Address, callback: Listener<Socket>) {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        val bS = ServerBootstrap()
        bS.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    public override fun initChannel(ch: SocketChannel) {
                        val res = handleChannel(ch)
                        callback(res)
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)


        bS.bind(self.port).sync()
    }

    // You generally don't want to be doing this from client code. That said, if you absolutely must interact
    // with the underlying netty channel, here you go!
    fun listenOnReturnChannel(self: Address, callback: Listener<Socket>): Channel {
        val bossGroup = NioEventLoopGroup()
        val workerGroup = NioEventLoopGroup()
        val bS = ServerBootstrap()
        bS.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    @Throws(Exception::class)
                    public override fun initChannel(ch: SocketChannel) {
                        val res = handleChannel(ch)
                        res.registerTypeListener(HandshakeMessage::class.java) {
                            it
                        }
                        callback(res)
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
        return bS.bind(self.port).sync().channel()
    }

    fun error(errorMessage: String): Serializable {
        return makeErrorMessage(errorMessage)
    }
}