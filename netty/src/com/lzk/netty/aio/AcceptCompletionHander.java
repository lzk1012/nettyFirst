package com.lzk.netty.aio;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AcceptCompletionHander implements 
	CompletionHandler<AsynchronousSocketChannel, AsynTimeServerHandler> {

	@Override
	public void completed(AsynchronousSocketChannel result, AsynTimeServerHandler attachment) {
		// 每当接收一个连接成功后，再异步接收新的客户端连接
		attachment.channel.accept(attachment, this);
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		// 又交由读的回调类处理
		result.read(buffer, buffer, new ReadCompletionHandler(result));
	}

	@Override
	public void failed(Throwable exc, AsynTimeServerHandler attachment) {
		exc.printStackTrace();
		// 失败后结束程序
		attachment.latch.countDown();
	}
}