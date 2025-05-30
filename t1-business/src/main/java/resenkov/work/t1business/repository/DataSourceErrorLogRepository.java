package resenkov.work.t1business.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import resenkov.work.t1business.entity.DataSourceErrorLog;

@Repository
public interface DataSourceErrorLogRepository extends JpaRepository<DataSourceErrorLog, Long> {
}