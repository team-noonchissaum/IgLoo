package noonchissaum.backend.domain.chat.repository;


import noonchissaum.backend.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);

    List<ChatMessage> findByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long cursor, Pageable pageable);
}
