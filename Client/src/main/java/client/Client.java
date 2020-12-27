package client;

import client.controllers.AuthController;
import client.controllers.ViewController;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class Client extends Application {

    public static final List<String> USERS_TEST_DATA = List.of("Oleg", "Alexey", "Peter");

    private ClientChatState state = ClientChatState.AUTHENTICATION;
    private Stage primaryStage;
    private Stage authDialogStage;
    private Network network;
    private ViewController viewController;

    public void updateUsers(List<String> users) {
        viewController.usersList.setItems(FXCollections.observableList(users));
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;

        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/view.fxml"));

        Parent root = loader.load();
        viewController = loader.getController();

        primaryStage.setTitle("Online server.chat");
        primaryStage.setScene(new Scene(root, 800, 600));
        viewController.getTextField().requestFocus();

        primaryStage.show();

        network = new Network(this);
        if (!network.connect()) {
            showNetworkError("", "Failed to connect to server");
        }

        viewController.setNetwork(network);

        network.waitMessages(viewController);

        primaryStage.setOnCloseRequest(event -> {
            try {
                network.sendEndMessage();
            } catch (IOException e) {
                e.printStackTrace();
            }
            network.close();
        });

        openAuthDialog();
    }

    private void openAuthDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/authDialog.fxml"));
        AnchorPane parent = loader.load();

        authDialogStage = new Stage();
        authDialogStage.initModality(Modality.WINDOW_MODAL);
        authDialogStage.initOwner(primaryStage);

        AuthController authController = loader.getController();
        authController.setNetwork(network);

        authDialogStage.setScene(new Scene(parent));
        authDialogStage.show();

        authDialogStage.setOnCloseRequest(event -> primaryStage.close());
    }

    public static void showNetworkError(String errorDetails, String errorTitle) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Network Error");
        alert.setHeaderText(errorTitle);
        alert.setContentText(errorDetails);
        alert.showAndWait();
    }

    public void activeChatDialog(String nickname) {
        primaryStage.setTitle(nickname);
        state = ClientChatState.CHAT;
        authDialogStage.close();
        primaryStage.show();
        viewController.getTextField().requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public ClientChatState getState() {
        return state;
    }

}
