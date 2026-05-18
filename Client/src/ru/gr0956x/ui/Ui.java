package ru.gr0956x.ui;

import ru.gr0956x.net.MessageType;

import java.awt.*;
import java.util.function.Consumer;

public interface Ui {
    void showInfo(String data, MessageType type);

    void addUserDataListener(Consumer<String> listener);
    void removeUserDataListener(Consumer<String> listener);
}
