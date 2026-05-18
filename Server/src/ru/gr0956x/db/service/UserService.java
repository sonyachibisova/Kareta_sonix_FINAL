package ru.gr0956x.db.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gr0956x.db.entity.User;
import ru.gr0956x.db.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public boolean isNickTaken(String nick) {
        return userRepository.existsByNickIgnoreCase(nick);
    }

    @Transactional
    public User register(String nick, String password) {
        var existing = userRepository.existsByNickIgnoreCase(nick);
        if (existing) {
            throw new IllegalArgumentException("Ник " + nick + " уже занят");
        }
        var passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        User newUser = new User(nick, passwordHash);
        return userRepository.save(newUser);
    }

    @Transactional
    public User login(String nick, String password) {
        Optional<User> existing = userRepository.findByNickIgnoreCase(nick);
        if (existing.isPresent()) {
            var user = existing.get();
            if (BCrypt.checkpw(password, user.getPasswordHash()))
                return existing.get();
        }
        throw new IllegalArgumentException("Неверный ник или пароль");
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
