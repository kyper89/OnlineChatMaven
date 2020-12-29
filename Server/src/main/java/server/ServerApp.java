package server;

import server.chat.MyServer;

import java.io.IOException;

public class ServerApp {

    public static final int SERVER_PORT = 8189;

    public static void main(String[] args) {
        int port = SERVER_PORT;
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        }
        try {
            new MyServer().start(port);
        } catch (Exception e) {
            System.err.println("Failed to create server.chat.MyServer");
            e.printStackTrace();
        }
    }

}
