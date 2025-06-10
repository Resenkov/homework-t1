package resenkov.work.t1unlockaccount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import resenkov.work.t1checktransaction.entity.Blacklist;

import java.util.List;

@Repository
public interface UnlockClientRepository extends JpaRepository<Blacklist, Long> {
    @Query(value = """
            SELECT * 
            FROM blacklist.public.blacklist 
            ORDER BY random() 
            LIMIT :limit
            """, nativeQuery = true)
    List<Blacklist> findRandomBlacklistEntries(@Param("limit") int limit);
}