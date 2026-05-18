package ru.gr0956x.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.gr0956x.db.entity.Message;
import ru.gr0956x.db.entity.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySenderOrReceiverOrderBySentAtDesc(User sender, User receiver);

    @Query("SELECT m FROM Message m WHERE m.content LIKE %:fragment% AND " +
            "(m.sender = :user OR m.receiver = :user)")
    List<Message> searchByContentFragment(@Param("fragment") String fragment, @Param("user") User user);
}