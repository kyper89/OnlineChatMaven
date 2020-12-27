package client;

import client.controllers.ViewController;
import clientserver.Command;
import clientserver.commands.*;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static clientserver.Command.*;

public class Network {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8189;

    private final String host;
    private final int port;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private Client clientChat;
    private String nickname;

    public Network() {
        this(SERVER_ADDRESS, SERVER_PORT);
    }

    public Network(Client clientChat) {
        this();
        this.clientChat = clientChat;
    }

    public Network(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            return true;
        } catch (IOException e) {
            System.err.println("Соединение не было установлено!");
            e.printStackTrace();
            return false;
        }
    }

    public void sendPrivateMessage(String receiver, String message) throws IOException {
        sendCommand(privateMessageCommand(receiver, message));
    }

    public void sendMessage(String message) throws IOException {
        sendCommand(publicMessageCommand(nickname, message));
    }

    private void sendCommand(Command command) throws IOException {
        outputStream.writeObject(command);
    }

    public void waitMessages(ViewController viewController) {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    Command command = readCommand();
                    if (command == null) {
                        continue;
                    }

                    if (clientChat.getState() == ClientChatState.AUTHENTICATION) {
                        processAuthResult(command);
                    } else {
                        processMessage(viewController, command);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Соединение было потеряно!");
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void processMessage(ViewController viewController, Command command) {
        switch (command.getType()) {
            case INFO_MESSAGE -> {
                MessageInfoCommandData data = (MessageInfoCommandData) command.getData();
                Platform.runLater(() -> viewController.appendMessage(data.getMessage()));
            }
            case CLIENT_MESSAGE -> {
                ClientMessageCommandData data = (ClientMessageCommandData) command.getData();
                String sender = data.getSender();
                String message = data.getMessage();
                Platform.runLater(() -> viewController.appendMessage(String.format("%s: %s", sender, message)));
            }
            case ERROR -> {
                ErrorCommandData data = (ErrorCommandData) command.getData();
                Platform.runLater(() -> Client.showNetworkError(data.getErrorMessage(), "Server error"));
            }
            case UPDATE_USERS_LIST -> {
                UpdateUsersListCommandData data = (UpdateUsersListCommandData) command.getData();
                Platform.runLater(() -> clientChat.updateUsers(data.getUsers()));
            }
            default -> throw new IllegalArgumentException("Unknown command type: " + command.getType());
        }
    }

    private void processAuthResult(Command command) {
        switch (command.getType()) {
            case AUTH_OK -> {
                AuthOkCommandData data = (AuthOkCommandData) command.getData();
                nickname = data.getUsername();
                Platform.runLater(() -> clientChat.activeChatDialog(nickname));
            }
            case ERROR -> {
                ErrorCommandData data = (ErrorCommandData) command.getData();
                Platform.runLater(() -> Client.showNetworkError(data.getErrorMessage(), "Auth error"));
            }
            default -> throw new IllegalArgumentException("Unknown command type: " + command.getType());
        }
    }

    private Command readCommand() throws IOException {
        Command command = null;
        try {
            command = (Command) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to read Command class");
            e.printStackTrace();
        }

        return command;
    }

    public void sendAuthMessage(String login, String password) throws IOException {
        sendCommand(authCommand(login, password));
    }

    public void sendEndMessage() throws IOException {
        sendCommand(endCommand());
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}