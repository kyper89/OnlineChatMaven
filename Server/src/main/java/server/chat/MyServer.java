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
import java.util.logging.*;

public class MyServer {

    private final List<ClientHandler> clients = new ArrayList<>();
    private final AuthService authService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private Logger logger;

    public MyServer() throws IOException {
        prepareLogger();
        this.authService = new SQLiteAuthService();
    }

    private void prepareLogger() throws IOException {
        this.logger = Logger.getLogger(MyServer.class.getName());
        this.logger.setLevel(Level.FINER);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINER);

        FileHandler fileHandler = new FileHandler("log.log", true);
        fileHandler.setLevel(Level.INFO);

        this.logger.addHandler(consoleHandler);
        this.logger.addHandler(fileHandler);
        this.logger.setUseParentHandlers(false);
    }

    public void start(int port) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.log(Level.INFO, "Start server");
            runServerMessageThread();
            authService.start();
            //noinspection InfiniteLoopStatement
            while (true) {
                waitAndProcessNewClientConnection(serverSocket);
            }
        } catch (IOException | SQLException e) {
            logger.log(Level.SEVERE, "Failed to accept new connection", e);
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
                    logger.log(Level.SEVERE, "Failed to process serverMessage", e);
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

    public Logger getLogger() {
        return logger;
    }

}