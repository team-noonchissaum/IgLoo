package noonchissaum.backend.domain.task.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;
import noonchissaum.backend.domain.task.dto.DbUpdateEvent;

@Entity
@Table(name = "tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AsyncTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,nullable = false)
    private String requestId;

    private boolean isSuccess;

    private Long userId;
    private Long auctionId;

    @Builder
    public AsyncTask(DbUpdateEvent event) {
        this.requestId = event.requestId();
        this.userId = event.userId();
        this.auctionId = event.auctionId();
        this.isSuccess = false;
    }

    public void taskSuccess() {
        this.isSuccess = true;
    }
}
