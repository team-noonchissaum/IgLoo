package noonchissaum.backend.domain.chatbot.repository;

import noonchissaum.backend.domain.chatbot.entity.ChatOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatOptionRepository extends JpaRepository<ChatOption, Long> {
    List<ChatOption> findByNodeIdOrderBySortOrderAsc(Long nodeId);
    Optional<ChatOption> findByIdAndNodeId(Long id, Long nodeId);
}
