package server.handler;

import clientserver.commands.ChangeNickCommandData;
import server.chat.MyServer;
import clientserver.Command;
import clientserver.CommandType;
import clientserver.commands.AuthCommandData;
import clientserver.commands.PrivateMessageCommandData;
import clientserver.commands.PublicMessageCommandData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import static clientserver.Command.*;

public class ClientHandler {

    private final MyServer myServer;
    private final Socket clientSocket;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    private String nickname;

    public ClientHandler(MyServer myServer, Socket clientSocket) {
        this.myServer = myServer;
        this.clientSocket = clientSocket;
    }

    public void handle() throws IOException {
        in  = new ObjectInputStream(clientSocket.getInputStream());
        out = new ObjectOutputStream(clientSocket.getOutputStream());

        try {
            authentication();
        } catch (IOException e) {
            System.out.println("Клиент отключился до аутентификации");
        }

        try {
            readMessages();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Непредвиденная ошибка чтения сообщений");
        } finally {
            try {
                closeConnection();
            } catch (IOException e) {
                System.err.println("Failed to close connection!");
            }
        }
    }

    private void authentication() throws IOException {

        waitAuth();

        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }

            if (command.getType() == CommandType.AUTH) {
                AuthCommandData data = (AuthCommandData) command.getData();
                String login = data.getLogin();
                String password = data.getPassword();

                String nickname = myServer.getAuthService().getNickByLoginPass(login, password);
                if (nickname == null) {
                    sendCommand(errorCommand("Некорректные логин или пароль!"));
                    continue;
                }

                if (myServer.isNickBusy(nickname)) {
                    sendCommand(errorCommand("Такой пользователь уже существует!"));
                    continue;
                }

                sendCommand(authOkCommand(nickname, login));
                setNickname(nickname);
                myServer.broadcastMessage(String.format("Пользователь '%s' зашел в чат!", nickname), null);
                myServer.subscribe(this);
                return;
            }
        }
    }

    private void waitAuth() {

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (nickname == null) {
                    try {
                        closeConnection();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.err.println("Ошибка закрытия соединения по истечению времени ожидания аутентификации");
                    }
                }
            }
        };

        new Timer().schedule(task, 120000L);
    }

    public void sendCommand(Command command) throws IOException {
        out.writeObject(command);
    }

    private Command readCommand() throws IOException {
        Command command = null;
        try {
            command = (Command) in.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to read clientserver.Command class");
            e.printStackTrace();
        }

        return command;
    }

    private void readMessages() throws Exception {
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }

            switch (command.getType()) {
                case PRIVATE_MESSAGE: {
                    PrivateMessageCommandData data = (PrivateMessageCommandData) command.getData();
                    String receiver = data.getReceiver();
                    String message = data.getMessage();
                    myServer.broadcastMessage(message, this, receiver );
                    break;
                }
                case PUBLIC_MESSAGE: {
                    PublicMessageCommandData data = (PublicMessageCommandData) command.getData();
                    String message = data.getMessage();
                    myServer.broadcastMessage(message, this);
                    break;
                }
                case CHANGE_NICK: {
                    ChangeNickCommandData data = (ChangeNickCommandData) command.getData();
                    String newNickname = data.getNewNickname();
                    if (myServer.isNickBusy(newNickname) || myServer.isNickBusyInDataBase(newNickname)) {
                        sendCommand(errorCommand("Этот ник уже занят!"));
                        break;
                    }
                    if (myServer.changeNick(newNickname, this)) {
                        sendCommand(changeNickOkCommand(newNickname));
                    }
                    break;
                }
                case END:
                    return;
                default:
                    throw new IllegalArgumentException("Unknown command type: " + command.getType());

            }
        }
    }

    private void closeConnection() throws IOException {
        myServer.unsubscribe(this);
        clientSocket.close();
    }

    public void sendMessage(String message) throws IOException {
        sendCommand(messageInfoCommand(message));
    }

    public void sendMessage(String sender, String message) throws IOException {
        sendCommand(clientMessageCommand(sender, message));
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}