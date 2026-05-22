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
    private String myNick = null;

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
                refreshChat();
            }
        });
        JScrollPane onlineScroll = new JScrollPane(onlineList);
        onlineScroll.setPreferredSize(new Dimension(150, 0));

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
        

        JPanel filterPanel = new JPanel(new GridLayout(1, 2));
        filterPanel.add(publicButton);
        filterPanel.add(privateButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(filterPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        publicButton.addActionListener(e -> {
            selectedPrivateNick = null;
            refreshChat();
        });

        privateButton.addActionListener(e -> {
            var selected = onlineList.getSelectedValue();
            if (selected != null) {
                selectedPrivateNick = selected;
                privateButton.setText("Личные: " + selected);
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
                if (type == MessageType.MESSAGE) {
                    var parts = data.split(":", 2);
                    if (parts.length == 2) {
                        allMessages.add(type + ":" + parts[0] + ":" + parts[1]);
                    }
                } else { // PRIVATE
                    var parts = data.split(":", 3);
                    if (parts.length == 3) {
                        // Формат: PRIVATE:sender:receiver:text
                        allMessages.add(type + ":" + parts[0] + ":" + parts[1] + ":" + parts[2]);
                    }
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
            } else if (type == MessageType.INFO) {
            // "Пользователь X вошел в чат"
            if (data.startsWith("Пользователь ") && data.endsWith(" вошел в чат")) {
                String nick = data.substring(12, data.length() - 12).trim();
                if (myNick == null && !nick.isBlank()) {
                    myNick = nick;
                }
            }
            chatArea.append(data + "\n");
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
            var typeAndRest = msg.split(":", 2);

            if (typeAndRest.length < 2) {
                continue;
            }

            var msgType = typeAndRest[0];
            var rest = typeAndRest[1];

            // ===== ОБЩИЙ ЧАТ =====
            if (msgType.equals("MESSAGE")) {

                if (selectedPrivateNick == null) {

                    var parts = rest.split(":", 2);

                    if (parts.length == 2) {
                        var sender = parts[0];
                        var text = parts[1];

                        chatArea.append(sender + ": " + text + "\n");
                    }
                }
            }

            // ===== ЛИЧНЫЕ СООБЩЕНИЯ =====
            else if (msgType.equals("PRIVATE")) {

                if (selectedPrivateNick != null) {

                    var parts = rest.split(":", 3);

                    if (parts.length == 3) {

                        var sender = parts[0];
                        var receiver = parts[1];
                        var text = parts[2];

                        boolean show = false;

                        // показываем ТОЛЬКО сообщения
                        // между мной и выбранным пользователем

                        if (myNick != null) {

                            boolean iAmSender =
                                    sender.equalsIgnoreCase(myNick);

                            boolean iAmReceiver =
                                    receiver.equalsIgnoreCase(myNick);

                            boolean selectedIsSender =
                                    sender.equalsIgnoreCase(selectedPrivateNick);

                            boolean selectedIsReceiver =
                                    receiver.equalsIgnoreCase(selectedPrivateNick);

                            show =
                                    (iAmSender && selectedIsReceiver) ||
                                            (iAmReceiver && selectedIsSender);
                        }

                        if (show) {
                            chatArea.append(sender + ": " + text + "\n");
                        }
                    }
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