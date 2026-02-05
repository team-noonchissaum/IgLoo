package noonchissaum.backend.domain.chat.repository;

import noonchissaum.backend.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ChatMessage m
           set m.isRead = true
         where m.room.id = :roomId
           and m.sender.id <> :userId
           and m.isRead = false
    """)
    //몇 건 읽음 처리됐는지
    int markAllAsReadInRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);
    List<ChatMessage> findByRoomIdOrderByIdDesc(Long roomId, Pageable pageable);

    List<ChatMessage> findByRoomIdAndIdLessThanOrderByIdDesc(Long roomId, Long cursor, Pageable pageable);
}
