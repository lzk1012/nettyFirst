package com.lzk.netty.aio;

public class TimeClient {
    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        new Thread(new AsynTimeClientHandler("127.0.0.1", port), "AIO CLIENT").start();
    }
}
