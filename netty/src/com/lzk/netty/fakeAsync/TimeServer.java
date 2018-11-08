package com.lzk.netty.fakeAsync;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.lzk.netty.bio.TimeServerHandler;

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
            System.out.println("The time server is start in port: " + port);
            Socket socket = null;
            //maxPoolSize, workQueueSize
            TimeServerHandlerExecutePool singleExecutor = 
            		new TimeServerHandlerExecutePool(50, 10000);
            while (true) {
                socket = server.accept();
                //使用的是原来com.lzk.netty.bio包中的TimeServerHandler
                singleExecutor.execute(new TimeServerHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (server != null) {
                System.out.println("The time server close");
                server.close();
                server = null;
            }
        }
    }
}
