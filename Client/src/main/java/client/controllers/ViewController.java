package client.controllers;

import client.Client;
import client.Network;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.IOException;

public class ViewController {

    @FXML
    public ListView<String> usersList;

    @FXML
    public Button buttonEnter;

    @FXML
    private TextField textField;

    @FXML
    private TextArea messagesArea;

    private Network network;
    private String selectedRecipient;

    @FXML
    public void initialize() {
        usersList.setItems(FXCollections.observableArrayList(Client.USERS_TEST_DATA));

        usersList.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = usersList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                usersList.requestFocus();
                if (! cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedRecipient = null;
                    } else {
                        selectionModel.select(index);
                        selectedRecipient = cell.getItem();
                    }
                    event.consume();
                } else {
                    selectionModel.clearSelection();
                    selectedRecipient = null;
                }
            });
            return cell ;
        });

        //textField.requestFocus();
    }

    @FXML
    public void sendMessage() {
        String enteredText = textField.getText();
        if (enteredText.equals("")) {
            return;
        }
        appendMessage("Я: " + enteredText);
        textField.clear();

        try {
            if (selectedRecipient != null) {
                network.sendPrivateMessage(selectedRecipient, enteredText);
            } else {
                network.sendMessage(enteredText);
            }
        } catch (IOException e) {
            e.printStackTrace();
            String errorMessage = "Failed to send message";
            Client.showNetworkError(e.getMessage(), errorMessage);
        }
    }

    public void appendMessage(String message) {
        messagesArea.appendText(message);
        messagesArea.appendText(System.lineSeparator());
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public TextField getTextField() {
        return textField;
    }

}
