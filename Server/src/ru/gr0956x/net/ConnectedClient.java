package ru.gr0956x.net;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import ru.gr0956x.db.service.MessageService;
import ru.gr0956x.db.service.UserService;
import ru.gr0956x.db.entity.User;

public class ConnectedClient {
    private final Communicator communicator;
    private final static List<ConnectedClient> clients = new ArrayList<>();
    private String name = null;
    private User currentUser = null;
    private final UserService userService;
    private boolean authorized = false;
    private final MessageService messageService;

    public ConnectedClient(Socket socket, UserService userService, MessageService messageService) {
        this.userService = userService;
        this.messageService = messageService;
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
        clients.add(this);
    }
    public void start(){
        communicator.start();
        sendData(MessageType.REQUEST + ":" + "Введите имя:");
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    private void parseData(String data) {
        //System.out.println("Получено: "+data);
        if (name == null) {
            if (data.isBlank()) {
                sendData(MessageType.ERROR
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Такое имя не подходит");
                sendData(MessageType.REQUEST
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Введите имя: ");
                return;
            }
            if (!data.matches("^[a-zA-Zа-яА-ЯёЁ].*")) {
                sendData(MessageType.ERROR
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Имя должно начинаться с буквы");
                sendData(MessageType.REQUEST
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + "Введите имя:");
                return;
            }
            name = data;
            if (userService.isNickTaken(name)) {
                sendData(MessageType.REQUEST + ProtocolConstants.COMMAND_SEPARATOR + "Введите пароль:");
            } else {
                sendData(MessageType.REQUEST + ProtocolConstants.COMMAND_SEPARATOR + "Придумайте пароль:");
            }

        } else {
            if (authorized) {
                if (data.startsWith("!поиск ")) {
                    var fragment = data.substring(7).trim();
                    sendSearchResults(fragment);
                    return;
                }
                if (data.startsWith("!личные ")) {
                    var targetName = data.substring(8).trim();
                    sendHistory(targetName, true);
                    return;
                }
                if (data.startsWith("!общие ")) {
                    var targetName = data.substring(7).trim();
                    sendHistory(targetName, false);
                    return;
                }
                if (data.startsWith("@")) {
                    var parts = data.split(" ", 2);
                    if (parts.length == 2) {
                        var targetName = parts[0].substring(1);
                        sendPrivate(targetName, parts[1]);
                    }
                } else {
                    sendForAll(MessageType.MESSAGE, data);
                }
                return;
            }
            try {
                User user;
                if (userService.isNickTaken(name)) {
                    user = userService.login(name, data);
                } else {
                    user = userService.register(name, data);
                }
                synchronized (clients) {
                    var alreadyOnline = clients.stream()
                            .filter(c -> c.name != null && c.name.equalsIgnoreCase(name) && c != this)
                            .findFirst();
                    if (alreadyOnline.isPresent()) {
                        sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Пользователь с таким именем уже в сети");
                        name = null;
                        sendData(MessageType.REQUEST + ProtocolConstants.COMMAND_SEPARATOR + "Введите имя:");
                        return;
                    }
                }
                authorized = true;
                currentUser = user;
                broadcastOnlineList();
                sendForAll(MessageType.INFO, "Пользователь " + name + " вошел в чат");
            } catch (IllegalArgumentException e) {
                sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + e.getMessage());
                name = null;
                sendData(MessageType.REQUEST + ProtocolConstants.COMMAND_SEPARATOR + "Введите имя:");
            }
        }
    }

    private void sendForAll(MessageType type, String data){
        var author = (type == MessageType.MESSAGE) ?
                name + ProtocolConstants.AUTHOR_SEPARATOR :
                "";
        synchronized (clients) {
            clients.stream()
                    .filter(c -> c.name != null)
                    .forEach(client -> {
                        client.sendData(type
                                + ProtocolConstants.COMMAND_SEPARATOR
                                + author
                                + data);
                    });
            if ((type == MessageType.MESSAGE || type == MessageType.PRIVATE) && currentUser != null) {
                messageService.save(currentUser, null, data);
            }
        }
    }

    private static void broadcastOnlineList() {
        synchronized (clients) {
            var onlineNicks = clients.stream()
                    .filter(c -> c.name != null)
                    .map(c -> c.name)
                    .toList();
            var list = String.join(",", onlineNicks);
            for (var client : clients) {
                if (client.name != null) {
                    client.sendData(MessageType.ONLINE + ProtocolConstants.COMMAND_SEPARATOR + list);
                }
            }
        }
    }

    private void sendPrivate(String targetName, String data) {
        synchronized (clients) {
            var target = clients.stream()
                    .filter(c -> c.name != null && c.name.equalsIgnoreCase(targetName))
                    .findFirst();
            if (target.isPresent()) {
                target.get().sendData(MessageType.PRIVATE
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + name
                        + ProtocolConstants.AUTHOR_SEPARATOR
                        + data);
                if (!target.get().name.equalsIgnoreCase(name)) {
                    sendData(MessageType.PRIVATE
                            + ProtocolConstants.COMMAND_SEPARATOR
                            + name
                            + ProtocolConstants.AUTHOR_SEPARATOR
                            + data);
                }
                if (currentUser != null && target.get().currentUser != null) {
                    messageService.save(currentUser, target.get().currentUser, data);
                }
            } else {
                sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Пользователь не в сети");
            }
        }
    }

    private void sendHistory(String targetName, boolean privateOnly) {
        var targetUser = userService.getAllUsers().stream()
                .filter(u -> u.getNick().equalsIgnoreCase(targetName))
                .findFirst();
        if (targetUser.isPresent() && currentUser != null) {
            var messages = messageService.getChatHistory(currentUser, targetUser.get());
            var lastMessages = messages.stream()
                    .filter(m -> privateOnly ? m.getReceiver() != null : m.getReceiver() == null)
                    .limit(10)
                    .toList();
            for (var msg : lastMessages) {
                var sender = msg.getSender().getNick();
                sendData(MessageType.HISTORY
                        + ProtocolConstants.COMMAND_SEPARATOR
                        + sender
                        + ProtocolConstants.AUTHOR_SEPARATOR
                        + msg.getContent());
            }
        }
    }

    private void sendSearchResults(String fragment) {
        if (currentUser != null) {
            var results = messageService.searchMessages(currentUser, fragment);
            if (results.isEmpty()) {
                sendData(MessageType.INFO + ProtocolConstants.COMMAND_SEPARATOR + "Ничего не найдено");
            } else {
                for (var msg : results) {
                    var sender = msg.getSender().getNick();
                    sendData(MessageType.SEARCH
                            + ProtocolConstants.COMMAND_SEPARATOR
                            + sender
                            + ProtocolConstants.AUTHOR_SEPARATOR
                            + msg.getContent());
                }
            }
        }
    }

    public void stop(){
        clients.remove(this);
        broadcastOnlineList();
        communicator.stop();
    }
}
