package noonchissaum.backend.domain.chatbot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import noonchissaum.backend.global.entity.BaseTimeEntity;

@Getter
@Entity
@Table(name = "chat_options")
@NoArgsConstructor
public class ChatOption extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "node_id", nullable = false)
    private ChatNode node;

    @Column(nullable = false, length = 200)
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_node_id")
    private ChatNode nextNode;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ChatActionType actionType = ChatActionType.NONE;

    @Column(name = "action_target", length = 500)
    private String actionTarget;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
