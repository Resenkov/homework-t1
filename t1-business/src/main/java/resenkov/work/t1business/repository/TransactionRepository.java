package resenkov.work.t1business.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import resenkov.work.t1business.entity.Account;
import resenkov.work.t1business.entity.Transaction;


import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    int countByAccountAndCreatedAtBetween(
            Account account,
            LocalDateTime start,
            LocalDateTime end
    );

    Transaction findByTranscationId(Long id);
}