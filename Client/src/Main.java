import ru.gr0956x.net.Client;
import ru.gr0956x.ui.MainFrame;


import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            var c = new Client("localhost", 9460);
            var ui = new MainFrame();
            ui.addUserDataListener(c::sendData); // подписка
            c.addDataListener(ui::showInfo);
            c.start();
            ui.setVisible(true);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
