package resenkov.work.t1business.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import resenkov.work.t1business.entity.TimeLimitLog;

public interface TimeLimitLogRepository extends JpaRepository<TimeLimitLog, Long> {
}