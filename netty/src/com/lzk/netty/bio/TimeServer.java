package com.lzk.netty.bio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TimeServer {

	public static void main(String[] args) throws IOException {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("use  default port");
			}
		}
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
			System.out.println("server is start in port:" + port);
			Socket socket;
			while (true) {
				socket = server.accept();
				// 此处可以不用异步线程处理
				new Thread(new TimeServerHandler(socket)).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (server != null) {
				System.out.println("time server close");
				server.close();
			}
		}
	}
}
