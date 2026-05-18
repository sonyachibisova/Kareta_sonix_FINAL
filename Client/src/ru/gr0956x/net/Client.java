package ru.gr0956x.net;

import javax.imageio.IIOException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Client {
    private Communicator communicator;
    private List<BiConsumer<String, MessageType>> listeners = new ArrayList<>();
    public Client(String host, int port) throws IOException {
        var socket = new Socket(host, port);
        communicator = new Communicator(socket);
        communicator.addDataListener(this::parseData);
    }

    public void addDataListener(BiConsumer<String, MessageType> listener){
        listeners.add(listener);
    }

    public void removeDataListener(BiConsumer<String, MessageType> listener){
        listeners.remove(listener);
    }

    public void start() {
        communicator.start();
    }

    public void parseData(String data) {
        var fullInfo = data.split(":", 2);
        if (fullInfo.length == 2) {
            var type = MessageType.valueOf(fullInfo[0]);
            for (var listener : listeners) {
                listener.accept(fullInfo[1], type);
            }
        }
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    public void stop() {
        communicator.stop();
    }
}
