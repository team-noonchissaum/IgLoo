package noonchissaum.backend.domain.chatbot.repository;

import noonchissaum.backend.domain.chatbot.entity.ChatNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatNodeRepository extends JpaRepository<ChatNode, Long> {
    Optional<ChatNode> findByScenarioIdAndRootTrue(Long scenarioId);
}
