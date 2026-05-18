package ru.gr0956x.ui;

import ru.gr0956x.net.MessageType;
import javax.swing.SwingUtilities;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import java.awt.GridLayout;



public class MainFrame extends JFrame implements Ui {

    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private DefaultListModel<String> onlineModel;
    private JList<String> onlineList;
    private final List<Consumer<String>> listeners = new ArrayList<>();
    private String selectedPrivateNick = null;
    private JButton publicButton;
    private JButton privateButton;
    private final List<String> allMessages = new ArrayList<>();
    private JTextField searchField;

    public MainFrame() {
        setTitle("Карета");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        onlineModel = new DefaultListModel<>();
        onlineList = new JList<>(onlineModel);
        onlineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        onlineList.addListSelectionListener(e -> {
            var nick = onlineList.getSelectedValue();
            if (nick != null && !e.getValueIsAdjusting()) {
                selectedPrivateNick = nick;
                privateButton.setText("Личные: " + nick);
                publicButton.setEnabled(true);
                privateButton.setEnabled(false);
                refreshChat();
            }
        });
        JScrollPane onlineScroll = new JScrollPane(onlineList);

        inputField = new JTextField();
        sendButton = new JButton("Отправить");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(chatScroll, BorderLayout.CENTER);
        add(onlineScroll, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        searchField = new JTextField();
        searchField.setToolTipText("Поиск по сообщениям");
        JButton searchButton = new JButton("Поиск");

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        searchButton.addActionListener(e -> {
            var fragment = searchField.getText().trim();
            if (!fragment.isEmpty()) {
                sendToServer("!поиск " + fragment);
            }
        });
        searchField.addActionListener(e -> searchButton.doClick());

        publicButton = new JButton("Общий чат");
        privateButton = new JButton("Личные");
        publicButton.setEnabled(false);

        JPanel filterPanel = new JPanel(new GridLayout(1, 2));
        filterPanel.add(publicButton);
        filterPanel.add(privateButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(filterPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        publicButton.addActionListener(e -> {
            selectedPrivateNick = null;
            publicButton.setEnabled(false);
            privateButton.setEnabled(true);
            refreshChat();
        });

        privateButton.addActionListener(e -> {
            if (selectedPrivateNick != null) {
                refreshChat();
            }
        });

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    private void sendMessage() {
        var text = inputField.getText().trim();
        if (!text.isEmpty()) {
            var msg = text;
            if (selectedPrivateNick != null) {
                msg = "@" + selectedPrivateNick + " " + text;
            }
            for (var listener : listeners) {
                listener.accept(msg);
            }
            inputField.setText("");
        }
    }

    @Override
    public void showInfo(String data, MessageType type) {
        SwingUtilities.invokeLater(() -> {
            if (type == MessageType.MESSAGE || type == MessageType.PRIVATE) {
                var parts = data.split(":", 2);
                if (parts.length == 2) {
                    allMessages.add(type + ":" + parts[0] + ":" + parts[1]);
                }
                if ((selectedPrivateNick == null && type == MessageType.MESSAGE) ||
                        (selectedPrivateNick != null && type == MessageType.PRIVATE)) {
                    refreshChat();
                }
            } else if (type == MessageType.ONLINE) {
                onlineModel.clear();
                for (var nick : data.split(",")) {
                    if (!nick.isBlank()) onlineModel.addElement(nick);
                }
            } else if (type == MessageType.SEARCH) {
                var parts = data.split(":", 2);
                if (parts.length == 2) {
                    chatArea.append("[Поиск] " + parts[0] + ": " + parts[1] + "\n");
                }
            } else if (type == MessageType.HISTORY) {
                var parts = data.split(":", 2);
                if (parts.length == 2) {
                    chatArea.append("[История] " + parts[0] + ": " + parts[1] + "\n");
                }
            } else {
                chatArea.append(data + "\n");
            }
        });
    }

    @Override
    public void addUserDataListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeUserDataListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    private void refreshChat() {
        chatArea.setText("");
        for (var msg : allMessages) {
            var parts = msg.split(":", 3);
            var msgType = parts[0];
            var sender = parts[1];
            var text = parts[2];
            if (selectedPrivateNick == null) {
                if (msgType.equals("MESSAGE")) {
                    chatArea.append(sender + ": " + text + "\n");
                }
            } else {
                if (msgType.equals("PRIVATE")) {
                    chatArea.append(sender + ": " + text + "\n");
                }
            }
        }
    }

    private void sendToServer(String text) {
        for (var listener : listeners) {
            listener.accept(text);
        }
    }
}