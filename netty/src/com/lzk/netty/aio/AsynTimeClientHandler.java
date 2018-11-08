package com.lzk.netty.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

public class AsynTimeClientHandler implements 
		CompletionHandler<Void, AsynTimeClientHandler>, Runnable {

	private AsynchronousSocketChannel client;
	private String host;
	private int port;
	private CountDownLatch latch;

	public AsynTimeClientHandler(String host, int port) {
		this.host = host;
		this.port = port;
		try {
			client = AsynchronousSocketChannel.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		latch = new CountDownLatch(1);
		// attachment是this
		client.connect(new InetSocketAddress(host, port), this, this);
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void completed(Void result, AsynTimeClientHandler attachment) {
		// 连接上服务器的回调方法
		byte[] req = "QUERY TIME".getBytes();
		final ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.put(req);
		buffer.flip();
		client.write(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
			@Override
			public void completed(Integer result, ByteBuffer byteBuffer) {
				// 写的回调方法，因为可能一次不能写完，所以要判断下
				if (byteBuffer.hasRemaining()) {
					String name = this.getClass().getName();
					System.out.println(name);
					// 继续写
					client.write(byteBuffer, byteBuffer, this);
				} else {
					final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
					client.read(readBuffer, readBuffer, new CompletionHandler<Integer, ByteBuffer>() {
						@Override
						public void completed(Integer result, ByteBuffer byteBuffer) {
							byteBuffer.flip();
							byte[] bytes = new byte[byteBuffer.remaining()];
							byteBuffer.get(bytes);
							String body;
							try {
								body = new String(bytes, "UTF-8");
								System.out.println("Now is:" + body);
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							} finally {
								latch.countDown();
							}
						}

						@Override
						public void failed(Throwable exc, ByteBuffer attachment) {
							try {
								client.close();
							} catch (IOException e) {
								e.printStackTrace();
							} finally {
								latch.countDown();
							}
						}
					});
				}
			}

			@Override
			public void failed(Throwable exc, ByteBuffer attachment) {
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			}
		});
	}

	@Override
	public void failed(Throwable exc, AsynTimeClientHandler attachment) {
		try {
			client.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			latch.countDown();
		}
	}
}
