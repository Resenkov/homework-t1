package resenkov.work.t1metricsstarter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import resenkov.work.t1metricsstarter.entity.TimeLimitLog;

@Repository
public interface TimeLimitLogRepository extends JpaRepository<TimeLimitLog, Long> {
}