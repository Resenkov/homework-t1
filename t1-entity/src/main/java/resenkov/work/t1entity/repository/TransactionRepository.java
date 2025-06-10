package resenkov.work.t1entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Transaction;

import java.time.LocalDateTime;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    int countByAccountAndCreatedAtBetween(
            Account account,
            LocalDateTime start,
            LocalDateTime end
    );

    int countByAccountAndStatus(Account account, Transaction.Status status);

    Transaction findByTranscationId(Long id);
}