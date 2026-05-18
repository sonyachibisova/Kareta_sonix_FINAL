package ru.gr0956x.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Communicator {
    private Socket socket;
    private boolean isActive;
    private List<Consumer<String>> dataListener = new ArrayList<>();

    public void addDataListener(Consumer<String> c){
        dataListener.add(c);
    }

    public void removeDataListener(Consumer<String> c){
        dataListener.remove(c);
    }

    public Communicator(Socket socket) {
        this.socket = socket;
    }

    public void start() {
        isActive = true;
        new Thread(()->{
            try (var br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                while (isActive){
                    var data = br.readLine();
                    if (data == null) throw new RuntimeException("Ошибка получения данных");
                    for (var dataListener : dataListener) {
                        dataListener.accept(data);
                    }
                }
            } catch (Exception e) {
                System.out.println("Ошибка чтения данных");
                stop();
            }
        }).start();
    }

    public void sendData(String data) {
        try {
            var pw = new PrintWriter(socket.getOutputStream());
            pw.println(data);
            pw.flush();
        } catch (Exception e) {
            System.out.println("Ошибка отправки данных");
            stop();
        }
    }

    public void stop(){
        isActive = false;
        try{
            socket.close();
        } catch (IOException _) {
        }
    }
}
