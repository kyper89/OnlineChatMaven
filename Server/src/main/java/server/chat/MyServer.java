package server.chat;

import server.auth.AuthService;
import clientserver.Command;
import server.auth.SQLiteAuthService;
import server.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyServer {

    private final List<ClientHandler> clients = new ArrayList<>();
    private final AuthService authService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public MyServer() {
        this.authService = new SQLiteAuthService();
    }

    public void start(int port) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер был запущен");
            runServerMessageThread();
            authService.start();
            //noinspection InfiniteLoopStatement
            while (true) {
                waitAndProcessNewClientConnection(serverSocket);
            }
        } catch (IOException | SQLException e) {
            System.err.println("Failed to accept new connection");
            e.printStackTrace();
        } finally {
            authService.stop();
        }
    }

    private void runServerMessageThread() {
        Thread serverMessageThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String serverMessage = scanner.next();
                try {
                    broadcastMessage("Сервер: " + serverMessage, null);
                } catch (IOException e) {
                    System.err.println("failed to process serverMessage");
                    e.printStackTrace();
                }
            }
        });
        serverMessageThread.setDaemon(true);
        serverMessageThread.start();
    }

    private void waitAndProcessNewClientConnection(ServerSocket serverSocket) throws IOException {
        System.out.println("Ожидание нового подключения....");
        Socket clientSocket = serverSocket.accept();
        System.out.println("Клиент подключился");

        executorService.execute(() -> {
            try {
                ClientHandler clientHandler = new ClientHandler(this, clientSocket);
                clientHandler.handle();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Ошибка установки связи с клиентом");
            }
        });
    }

    public synchronized void broadcastMessage(String message, ClientHandler sender) throws IOException {

        for (ClientHandler client : clients) {
            if (client == sender) {
                continue;
            }

            if (sender == null) {
                client.sendMessage(message);
            } else {
                client.sendMessage(sender.getNickname(), message);
            }
        }
    }

    public void broadcastMessage(String message, ClientHandler sender, String recipientNick) throws IOException {
        ClientHandler recipient = getClientByNick(recipientNick);
        if (recipient == null) {
            sender.sendMessage("Пользователь с ником '" + recipientNick + "' не в сети");
        } else {
            recipient.sendMessage(sender.getNickname(), message);
        }
    }

    public synchronized void subscribe(ClientHandler handler) throws IOException {
        clients.add(handler);
        notifyClientsUsersListUpdated(clients);
    }

    public synchronized void unsubscribe(ClientHandler handler) throws IOException{
        clients.remove(handler);
        notifyClientsUsersListUpdated(clients);
    }

    private void notifyClientsUsersListUpdated(List<ClientHandler> clients) throws IOException {
        List<String> usernames = new ArrayList<>();
        for (ClientHandler client : clients) {
            usernames.add(client.getNickname());
        }

        for (ClientHandler client : clients) {
            client.sendCommand(Command.updateUsersListCommand(usernames));
        }
    }

    public AuthService getAuthService() {
        return authService;
    }

    public synchronized boolean isNickBusy(String nickname) {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(nickname)) {
                return true;
            }
        }

        return false;
    }

    public boolean isNickBusyInDataBase(String nickname) {
        return authService.isNickBusy(nickname);
    }

    public synchronized ClientHandler getClientByNick(String nickname) {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(nickname)) {
                return client;
            }
        }
        return null;
    }

    public synchronized boolean changeNick(String newNickname, ClientHandler sender) throws IOException {

        String currentNickname = sender.getNickname();
        boolean success = authService.changeNick(currentNickname, newNickname);
        if (success) {
            broadcastMessage(String.format("Пользователь '%s' изменил ник на '%s'!", currentNickname, newNickname), null);
            sender.setNickname(newNickname);
            notifyClientsUsersListUpdated(clients);
        }

        return success;
    }

}