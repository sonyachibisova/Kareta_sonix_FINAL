import ru.gr0956x.net.Server;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.gr0956x.db.DatabaseConfig;
import ru.gr0956x.db.service.UserService;
import ru.gr0956x.db.service.MessageService;

public class Main {
    public static void main(String[] args) {
        var context = new AnnotationConfigApplicationContext(DatabaseConfig.class);
        var userService = context.getBean(UserService.class);
        var messageService = context.getBean(MessageService.class);

        var s = new Server(9460, userService, messageService);
    }
}
