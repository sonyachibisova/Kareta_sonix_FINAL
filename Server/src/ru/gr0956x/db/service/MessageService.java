package ru.gr0956x.db.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.gr0956x.db.entity.Message;
import ru.gr0956x.db.entity.User;
import ru.gr0956x.db.repository.MessageRepository;

import java.util.List;

@Service
public class MessageService {
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Message save(User sender, User receiver, String content) {
        Message message = new Message(sender, receiver, content);
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getChatHistory(User user1, User user2) {
        var messages = messageRepository.findBySenderOrReceiverOrderBySentAtDesc(user1, user2);
        messages.forEach(m -> {
            m.getSender().getNick(); // инициализация LAZY в транзакции
            if (m.getReceiver() != null) {
                m.getReceiver().getNick(); //инициализация LAZY в транзакции
            }
        });
        return messages;
    }

    @Transactional(readOnly = true)
    public List<Message> searchMessages(User user, String fragment) {
        var results = messageRepository.searchByContentFragment(fragment, user);
        results.forEach(m -> m.getSender().getNick());
        return results;
    }
}