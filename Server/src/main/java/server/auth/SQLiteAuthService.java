package server.auth;

import server.SQL.SQLite;
import java.sql.SQLException;

public class SQLiteAuthService implements AuthService{

    private SQLite sqLite;

    @Override
    public void start() throws Exception {
        this.sqLite = new SQLite();
        this.sqLite.connect();
    }

    @Override
    public void stop() {
        this.sqLite.disconnect();
    }

    @Override
    public String getNickByLoginPass(String login, String password) {
        try {
            return this.sqLite.authQuery(login, password);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Ошибка выполнения операции с базой данных");
            return null;
        }
    }

    @Override
    public boolean changeNick(String currentNick, String newNick) {
        return sqLite.updateNick(currentNick, newNick);
    }

    @Override
    public boolean isNickBusy(String newNick) {
        return sqLite.isNickBusy(newNick);
    }
}
