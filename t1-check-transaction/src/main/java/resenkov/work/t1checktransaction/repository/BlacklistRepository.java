package resenkov.work.t1checktransaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import resenkov.work.t1checktransaction.entity.Blacklist;

import java.util.List;

public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {
    boolean existsByClientId(Long clientId);
    @Query(value = """
            SELECT * 
            FROM blacklist.public.blacklist 
            ORDER BY random() 
            LIMIT :limit
            """, nativeQuery = true)
    List<Blacklist> findRandomBlacklistEntries(@Param("limit") int limit);

    void deleteByClientId(Long clientId);
}