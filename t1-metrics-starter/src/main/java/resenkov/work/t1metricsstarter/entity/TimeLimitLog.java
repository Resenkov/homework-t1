package resenkov.work.t1metricsstarter.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "time_limit_log")
public class TimeLimitLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    private String methodName;
    private long duration;
    private LocalDateTime timestamp;
}
