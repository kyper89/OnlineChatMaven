package clientserver.commands;

import java.io.Serializable;

public class AuthOkCommandData implements Serializable {

    private final String username;
    private final String login;

    public AuthOkCommandData(String username, String login) {
        this.username = username;
        this.login = login;
    }

    public String getUsername() {
        return username;
    }

    public String getLogin() {
        return login;
    }
}