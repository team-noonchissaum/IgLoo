package noonchissaum.backend.domain.chatbot.repository;

import noonchissaum.backend.domain.chatbot.entity.ChatScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatScenarioRepository extends JpaRepository<ChatScenario, Long> {
    List<ChatScenario> findAllByActiveTrueOrderByIdAsc();
}
