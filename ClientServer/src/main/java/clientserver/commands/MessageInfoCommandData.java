package clientserver.commands;

import java.io.Serializable;

public class MessageInfoCommandData implements Serializable {

    private final String message;

    public MessageInfoCommandData(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
