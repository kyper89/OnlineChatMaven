package server.SQL;

import java.sql.*;

public class SQLite {
    private static Connection connection;
    private static PreparedStatement psAuth;
    private static PreparedStatement psFindNick;
    private static PreparedStatement psUpdateNick;

    public String authQuery(String login, String password) throws SQLException {

        String nick = null;
        psAuth.setString(1, login);
        psAuth.setString(2, password);

        ResultSet rs = psAuth.executeQuery();
        if (rs.next()) {
           nick = rs.getString("nick");
        }
        rs.close();

        return nick;
    }

    public Boolean isNickBusy(String newNick) {

        boolean isNickBusy;
        try {
            psFindNick.setString(1, newNick);
            ResultSet rs = psFindNick.executeQuery();
            isNickBusy = rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Ошибка проверки занятости ника в базе данных");
            isNickBusy = true;
        }

        return isNickBusy;
    }

    public Boolean updateNick(String currentNick, String newNick) {

        boolean success;
        try {
            psUpdateNick.setString(1, newNick);
            psUpdateNick.setString(2, currentNick);

            success = psUpdateNick.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Ошибка смены ника");
            success = false;
        }

        return success;
    }

    private void prepareAllStatements() throws SQLException {
        psAuth = connection.prepareStatement("SELECT nick FROM users WHERE login = ? AND pass = ?;");
        psFindNick = connection.prepareStatement("SELECT nick FROM users WHERE nick = ?;");
        psUpdateNick = connection.prepareStatement("UPDATE users SET nick = ? WHERE nick = ?;");
    }

    public void connect() throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        prepareAllStatements();
    }

    public void disconnect() {
        try {
            psAuth.close();
            psUpdateNick.close();
        } catch (SQLException e) {
            System.out.println("Ошибка закрытия подготовленного Statement");
            e.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("Ошибка закрытия соединения с базой данных");
            e.printStackTrace();
        }
    }
}
