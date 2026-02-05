package noonchissaum.backend.domain.chatbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.global.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name = "chat_scenarios")
@NoArgsConstructor
public class ChatScenario extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scenario_key", nullable = false, unique = true, length = 100)
    private String scenarioKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
