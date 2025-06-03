package resenkov.work.t1checktransaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import resenkov.work.t1checktransaction.entity.Blacklist;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {
    boolean existsByClientId(Long clientId);

}