package com.htuy.netlib.serials

import com.htuy.common.ObjectSerializer
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.handler.codec.serialization.ClassResolver
import java.io.Serializable
import java.io.StreamCorruptedException

/**
 * Note that this doesn't actually work currently and cannot be used. I expect to fix it at some point.
 */

class FstEnc : MessageToByteEncoder<Serializable>() {
    private val LENGTH_PLACEHOLDER = ByteArray(4)
    override fun encode(ctx: ChannelHandlerContext, msg: Serializable, out: ByteBuf) {
        val startIdx = out.writerIndex()
        val bout = ByteBufOutputStream(out)
        bout.write(LENGTH_PLACEHOLDER)
        out.writeBytes(ObjectSerializer.fstObjToBytes(msg))
        val endIdx = out.writerIndex()
        out.setInt(startIdx, endIdx - startIdx - 4)
    }
}

class FstDec : LengthFieldBasedFrameDecoder(1048576,0,4,0,4){


    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf): Any? {
        val frame = super.decode(ctx, `in`) as ByteBuf? ?: return null
        val bytes = ByteArray(`in`.readableBytes())
        `in`.getBytes(`in`.readerIndex(), bytes)

        return ObjectSerializer.fstObjectFromBytes(bytes)
    }

    protected override fun extractFrame(ctx: ChannelHandlerContext, buffer: ByteBuf, index: Int, length: Int): ByteBuf {
        return buffer.slice(index, length)
    }
}