package client.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import client.Client;
import client.Network;

import java.io.IOException;

public class AuthController {

    @FXML
    public TextField loginField;

    @FXML
    public PasswordField passwordField;

    private Network network;

    @FXML
    public void executeAuth() {
        String login = loginField.getText();
        String password = passwordField.getText();
        if (login == null || login.isBlank() || password == null || password.isBlank()) {
            Client.showNetworkError("Логин и пароль обязательны!", "Валидация");
            return;
        }

        try {
            network.sendAuthMessage(login, password);
        } catch (IOException e) {
            Client.showNetworkError(e.getMessage(), "Auth error!");
            e.printStackTrace();
        }
    }

    public void setNetwork(Network network) {
        this.network = network;
    }
}
