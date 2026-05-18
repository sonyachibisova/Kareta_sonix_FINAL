package ru.gr0956x.net;

import java.io.IOException;
import java.net.ServerSocket;

import ru.gr0956x.db.service.MessageService;
import ru.gr0956x.db.service.UserService;

public class Server {
    private boolean isActive;
    public Server(int port, UserService userService, MessageService messageService) {
        isActive = true;
        new Thread(()->{
            try (var serverSocket = new ServerSocket(port)) { //внутри трай значит закроется авотматически (отчистится)
                System.out.println("Сервер запустился");
                while (isActive) {
                    try {
                        var socket = serverSocket.accept();
                        System.out.println("Клиент подключен");
                        var connClient = new ConnectedClient(socket, userService, messageService);
                        connClient.start();
                    } catch (Exception e) {
                        System.out.println("Ошибка подключения клиентов");
                        isActive = false;
                    }
                }
            } catch (IOException e) {
                System.out.println("Ошибка включения сервера");
            }
        }).start();
    }
}

