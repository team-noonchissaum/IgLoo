package noonchissaum.backend.domain.chat.repository;

import noonchissaum.backend.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByAuctionId(Long auctionId);

    @Query("select cr " +
        "from ChatRoom cr " +
        "where cr.buyer.id = :userId or cr.seller.id = :userId " +
        "order by cr.createdAt desc "
    )
    List<ChatRoom> findMyRooms(@Param("userId") Long userId);

    @Query("""
    select (count(cr) > 0)
    from ChatRoom cr
    where cr.id = :roomId
      and (cr.buyer.id = :userId or cr.seller.id = :userId)
""")
    boolean isMember(@Param("roomId") Long roomId, @Param("userId") Long userId);
}
