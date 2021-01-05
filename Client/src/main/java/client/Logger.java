package client;

import client.controllers.ViewController;

import java.io.*;


public class Logger {

    public static final int NUMBER_OF_HISTORY_LINE_OF_LOAD = 100;
    private final String fileName;

    public Logger(String nickname) {
        this.fileName = String.format("history_%s.txt", nickname);
    }

    public void addMessageInHistory(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(this.fileName, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Не удалось записать сообщение в историю");
        }
    }

    public void showHistory(ViewController viewController) {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.fileName))) {

            Object[] arrLines = reader.lines().toArray();
            int count = arrLines.length > NUMBER_OF_HISTORY_LINE_OF_LOAD ? arrLines.length - NUMBER_OF_HISTORY_LINE_OF_LOAD : 0;
            for (int i = count; i < arrLines.length; i++) {
                viewController.appendMessage(String.valueOf(arrLines[i]));
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Не удалось загрузить сообщения из истории");
        }
    }

}
