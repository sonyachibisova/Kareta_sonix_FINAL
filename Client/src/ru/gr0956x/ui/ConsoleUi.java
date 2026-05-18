package ru.gr0956x.ui;

import ru.gr0956x.net.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

public class ConsoleUi implements Ui {

    private List<Consumer<String>> listeners = new ArrayList<>();

    public void start(){
        var scanner = new Scanner(System.in);
        new Thread(() -> {
            while (true){
                var userData = scanner.nextLine();
                for (var listener : listeners) {
                    listener.accept(userData);
                }
            }
        }).start();
    }

    @Override
    public void showInfo(String data, MessageType type) {
        switch (type){
            case MESSAGE -> {
                var message = data.split(":", 2);
                System.out.println(message[0]+" написал: ");
                System.out.println(message[1]);
            }
            case ERROR -> {
                System.err.println(data);
            }
            case ONLINE -> {
                System.out.println("В сети: " + data);
            }
            case PRIVATE -> {
                var message = data.split(":", 2);
                System.out.println("[ЛС] " + message[0] + ": " + message[1]);
            }
            case HISTORY -> {
                var message = data.split(":", 2);
                System.out.println("[История] " + message[0] + ": " + message[1]);
            }
            case SEARCH -> {
                var message = data.split(":", 2);
                System.out.println("[Найдено] " + message[0] + ": " + message[1]);
            }
            default -> {
                System.out.println(data);
            }
        }
    }

    @Override
    public void addUserDataListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeUserDataListener(Consumer<String> listener) {
        listeners.remove(listener);
    }
}
