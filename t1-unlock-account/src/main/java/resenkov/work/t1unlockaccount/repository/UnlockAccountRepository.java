package resenkov.work.t1unlockaccount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import resenkov.work.t1business.entity.Account;

import java.util.List;

public interface UnlockAccountRepository extends JpaRepository<Account,Long> {

    @Query(value = """
    SELECT * FROM account
    WHERE account.status = 'BLOCKED' OR account.status = 'ARRESTED'
    ORDER BY random()
    LIMIT :limit
""", nativeQuery = true)
    List<Account> findBlockedAccountsRandomly(@Param("limit") int limit);

}
