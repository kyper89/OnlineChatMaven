package server.auth;

public interface AuthService {

    void start() throws Exception;
    void stop();

    String getNickByLoginPass(String login, String password);

    boolean changeNick(String currentNick, String newNick);

    boolean isNickBusy(String newNick);

}