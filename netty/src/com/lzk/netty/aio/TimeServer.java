package com.lzk.netty.aio;

public class TimeServer {
	public static void main(String[] args) {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
				System.out.println("use  default port");
			}
		}

		AsynTimeServerHandler handler = new AsynTimeServerHandler(port);
		new Thread(handler, "AIO SERVER").start();
	}
}