package com.lzk.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimerServer implements Runnable {

	private Selector selector;
	private ServerSocketChannel serverSocketChannel;
	private volatile boolean stop;

	public MultiplexerTimerServer(int port) {
		try {
			//步骤一：打开ServerSocketChannel，用于监听客户端的连接
			serverSocketChannel = ServerSocketChannel.open();
			//步骤二：设置连接为非阻塞模式
			serverSocketChannel.configureBlocking(false);
			//并绑定监听端口，第二个参数backlog为请求队列的最大数量
			serverSocketChannel.socket().bind(new InetSocketAddress(port), 1024);
			//步骤三：新开辟一个线程，创建多路复用器并启动线程（TimeServer第14.15行）
			selector = Selector.open();//创建一个Selector，用于管理Channel
			//步骤四：将ServerSocketChannel注册到多路复用器Selector上，监听Accept事件
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			e.printStackTrace();
			// 表示程序非正常退出
			System.exit(1);

		}
	}

	public void stop() {
		this.stop = true;
	}

	@Override
	public void run() {
		while (!stop) {
			try {
				//步骤五：多路复用器在线程run方法的无限循环体内轮询准备就绪的Key
				// 每隔1s返回一次
				selector.select(1000);
				Set<SelectionKey> selectionKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectionKeys.iterator();
				SelectionKey key = null;
				while (it.hasNext()) {
					key = it.next();
					it.remove();
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
		if(key.isValid()){
			// 处理新接入的请求消息
			if (key.isAcceptable()) {
				ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
				//步骤六：多路复用器监听到有新的客户端接入，处理新的接入请求，完成TCP三次握手，建立物理电路
				SocketChannel sc = ssc.accept();
				//步骤七：设置客户端链路是非阻塞模式
				sc.configureBlocking(false);
				//步骤八：将新接入的客户端连接注册到Reactor线程的多路复用器上，监听客户端发送的消息
				sc.register(selector, SelectionKey.OP_READ);
			}
			// 读数据
			if (key.isReadable()) {
				SocketChannel sc = (SocketChannel) key.channel();
				ByteBuffer buffer = ByteBuffer.allocate(1024);
				//步骤九：异步读取客户端请求消息到缓存区
				int readBytes = sc.read(buffer);
				if (readBytes > 0) {
					// 为了读取数据
					buffer.flip();
					byte[] bytes = new byte[buffer.remaining()];
					buffer.get(bytes);
					String body = new String(bytes, "UTF-8");
					System.out.println("The time server receive order: " + body);
					String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? 
							new Date(System.currentTimeMillis()).toString() : "BAD ORDER";
					doWrite(sc, currentTime);
				}else if(readBytes<0){
					//对端链路关闭
					key.cancel();
					sc.close();
				}else{
					//读到0字节，忽略
				}
			}
		}
	}

	private void doWrite(SocketChannel sc, String response) throws IOException {
		if (response != null && response.trim().length() > 0) {
			byte[] bytes = response.getBytes();
			ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
			writeBuffer.put(bytes);
			// 写完之后调用flip
			writeBuffer.flip();
			//调用SocketChannel的异步write方法，将消息一步发送给客户端
			sc.write(writeBuffer);
		}
	}
}
