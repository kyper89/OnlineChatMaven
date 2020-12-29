package clientserver.commands;

import java.io.Serializable;

public class ChangeNickOkCommandData implements Serializable {

    private final String newNickname;

    public ChangeNickOkCommandData(String nickName) {
        this.newNickname = nickName;
    }

    public String getNickName() {
        return newNickname;
    }
}
