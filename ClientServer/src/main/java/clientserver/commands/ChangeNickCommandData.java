package clientserver.commands;

import java.io.Serializable;

public class ChangeNickCommandData implements Serializable {

    private final String newNickname;

    public ChangeNickCommandData(String newNickname) {
        this.newNickname = newNickname;
    }

    public String getNewNickname() {
        return newNickname;
    }
}
