package noonchissaum.backend.domain.chatbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.global.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name = "chat_nodes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_node_key", columnNames = {"scenario_id", "node_key"})
        })
@NoArgsConstructor
public class ChatNode extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private ChatScenario scenario;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "is_root", nullable = false)
    private boolean root;

    @Column(name = "is_terminal", nullable = false)
    private boolean terminal;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
