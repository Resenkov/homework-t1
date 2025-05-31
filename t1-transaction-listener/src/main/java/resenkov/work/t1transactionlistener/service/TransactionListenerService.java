package resenkov.work.t1transactionlistener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resenkov.work.t1business.entity.Account;
import resenkov.work.t1business.entity.Transaction;
import resenkov.work.t1business.repository.AccountRepository;
import resenkov.work.t1business.repository.TransactionRepository;
import resenkov.work.t1transactionlistener.dto.AcceptedTransactionMessage;
import resenkov.work.t1transactionlistener.dto.TransactionMessage;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
public class TransactionListenerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String ACCEPT_TOPIC = "t1_demo_transaction_accept";


    public TransactionListenerService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "t1_demo_transactions",
            groupId = "transaction-processor",
            containerFactory = "kafkaListenerContainerFactory"
    )

    @Transactional
    public void listen(TransactionMessage txMsg) {
        Long incomingAccountId = txMsg.getAccountId();
        Long incomingClientId = txMsg.getClientId();
        BigDecimal amount = txMsg.getAmount();
        Long incomingTxId = txMsg.getTransactionId();
        LocalDateTime messageTime = txMsg.getTimestamp();

        Optional<Account> optionalAccount = Optional.ofNullable(
                accountRepository.findByAccountId(incomingAccountId)
        );
        if (optionalAccount.isEmpty()) {
            log.warn("Не найден аккаунт с " + incomingAccountId + ". Транзакция " + incomingTxId + " проигнорирована");
            return;
        }

        Account account = optionalAccount.get();

        if (!Account.Status.OPEN.equals(account.getStatus())) {
            log.warn("Транзакция %d проигнорирована! Статус аккаунта %d не является OPEN!");
            return;
        }

        Transaction trx = new Transaction();
        trx.setTranscationId(incomingTxId);
        trx.setAccount(account);
        trx.setStatus(Transaction.Status.REQUESTED);
        trx.setSum(amount);
        transactionRepository.save(trx);

        BigDecimal oldBalance = (account.getBalance() == null)
                ? BigDecimal.ZERO
                : account.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        AcceptedTransactionMessage acceptedMsg = new AcceptedTransactionMessage(
                incomingClientId,
                incomingAccountId,
                incomingTxId,
                messageTime,
                amount,
                newBalance
        );
        try {
            String json = objectMapper.writeValueAsString(acceptedMsg);
            kafkaTemplate.send(ACCEPT_TOPIC, json);
            System.out.printf(
                    "[INFO ] Отправлено подтверждение txId=%d, accountId=%d, новый баланс=%s%n",
                    incomingTxId, incomingAccountId, newBalance
            );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
