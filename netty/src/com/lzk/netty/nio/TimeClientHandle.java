package com.lzk.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class TimeClientHandle implements Runnable {

	private String host;
	private int port;
	private Selector selector;
	private SocketChannel socketChannel;
	private volatile boolean stop;

	public TimeClientHandle(String host, int port) {
		this.host = host;
		this.port = port;
		try {
			//步骤一：打开SocketChannel，绑定客户端本地地址（默认系统会随机分配一个可用的本地地址）
			socketChannel = SocketChannel.open();
			//步骤二：设置SocketChannel为非阻塞模式
			socketChannel.configureBlocking(false);
			selector = Selector.open();//创建一个Selector，用于管理Channel
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	//步骤五：开辟新线程，创建多路复用器并启动线程（TimeClient中第13、14行）
	public void run() {
		try {
			doConnect();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while (!stop) {
			try {
				//步骤六：多路复用器在线程run方法内无线循环，轮询准备就绪的key
				selector.select(1000);
				Set<SelectionKey> selectionKeys = selector.selectedKeys();
				Iterator<SelectionKey> iterator = selectionKeys.iterator();
				SelectionKey key = null;
				while (iterator.hasNext()) {
					key = iterator.next();
					iterator.remove();
					try {
						handleInput(key);
					} catch (Exception e) {
						if (key != null) {
							if (key.channel() != null) {
								key.channel().close();
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		if (selector != null) {
			try {
				selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleInput(SelectionKey key) throws IOException {
		if (key.isValid()) {
			SocketChannel sc = (SocketChannel) key.channel();
			//步骤七：接收connect事件进行处理
			//判断是否可以连接,因为连接是异步的，连接的时候不一定连接上
			if (key.isConnectable()) {
				// 步骤八：如果连接成功，注册读事件到多路复用器
				if (sc.finishConnect()) {
					sc.register(selector, SelectionKey.OP_READ);
					doWrite(sc);
					System.out.println("connected");
				} else {
					System.exit(1);
				}
			}

			if (key.isReadable()) {
				ByteBuffer readBuffer = ByteBuffer.allocate(1024);
				//步骤九：异步读客户端请求消息到缓存区
				int read = sc.read(readBuffer);
				if (read > 0) {
					readBuffer.flip();
					byte[] bytes = new byte[readBuffer.remaining()];
					readBuffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("Now is: " + body);
					this.stop = true;
				} else if (read < 0) {
					key.cancel();
					sc.close();
				} else {
					System.out.println("get zero bytes");
				}
			}
		}
	}

	private void doConnect() throws IOException {
		//步骤三：尝试异步连接服务器
		if (socketChannel.connect(new InetSocketAddress(host, port))) {
			//步骤四，连接成功，直接注册读状态位到多路复用器中
			socketChannel.register(selector, SelectionKey.OP_READ);
			doWrite(socketChannel);
		} else {
			// 如果没有连接成功，处理的时候再进行连接
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}
	}

	private void doWrite(SocketChannel socketChannel) throws IOException {
		byte[] req = "QUERY TIME ORDER".getBytes();
		ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
		writeBuffer.put(req);
		writeBuffer.flip();
		socketChannel.write(writeBuffer);
		if (!writeBuffer.hasRemaining()) {
			System.out.println("Send order to server succeed.");
		}
	}
}
