package resenkov.work.t1transactionlistener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resenkov.work.t1business.entity.Account;
import resenkov.work.t1business.entity.Transaction;
import resenkov.work.t1business.repository.AccountRepository;
import resenkov.work.t1business.repository.TransactionRepository;
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

        Optional<Account> optionalAccount = accountRepository.findByAccountId(incomingAccountId);
        if (optionalAccount.isEmpty()) {
            log.warn("Аккаунт не найден: {}. Транзакция {} проигнорирована",
                    incomingAccountId, incomingTxId);
            return;
        }

        Account account = optionalAccount.get();


        if (account.getClient().getClientId() == null) {
            log.error("Не удалось получить ID клиента для этого аккаунта: {}", incomingAccountId);
            return;
        }

        if (!account.getClient().getClientId().equals(incomingClientId)) {
            log.warn("Учетная запись {} не принадлежит клиенту {}. Транзакция {} проигнорирована",
                    incomingAccountId, incomingClientId, incomingTxId);
            return;
        }

        if (!Account.Status.OPEN.equals(account.getStatus())) {
            log.warn("Статус счета {} не OPEN. Транзакция {} проигнорирована",
                    incomingAccountId, incomingTxId);
            return;
        }

        Transaction trx = new Transaction();
        trx.setTranscationId(incomingTxId);
        trx.setAccount(account);
        trx.setStatus(Transaction.Status.REQUESTED);
        trx.setSum(amount);
        transactionRepository.save(trx);

        BigDecimal oldBalance = Optional.ofNullable(account.getBalance())
                .orElse(BigDecimal.ZERO);
        BigDecimal newBalance = oldBalance.add(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        TransactionMessage acceptedMsg = new TransactionMessage(
                incomingTxId,
                incomingAccountId,
                incomingClientId,
                amount,
                Transaction.Status.REQUESTED,
                messageTime
        );
        try {
            String json = objectMapper.writeValueAsString(acceptedMsg);
            kafkaTemplate.send(ACCEPT_TOPIC, json);
            log.info("Транзакция {} принята и переадресована", incomingTxId);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать транзакцию: {}", incomingTxId, e);
        }
    }

    @KafkaListener(topics = "t1_demo_transaction_result",
            groupId = "transaction-result-processor",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void updateAfter(TransactionMessage message) {
        Long transactionId = message.getTransactionId();
        Long accountId = message.getAccountId();

        Optional<Transaction> resulttx = Optional.ofNullable(
                transactionRepository.findByTranscationId(transactionId)
        );

        Optional<Account> checkAccount = accountRepository.findByAccountId(accountId);
        if (resulttx.isEmpty() || checkAccount.isEmpty()) {
            log.warn("Транзакция в сообщении не найдена: {}", message);
            return;
        }

        Transaction transaction = resulttx.get();
        Account account = checkAccount.get();

        transaction.setStatus(message.getStatus());

        switch (message.getStatus()) {
            case ACCEPTED -> {
            }
            case BLOCKED -> {
                account.setStatus(Account.Status.BLOCKED);
                account.setFrozenAmount(account.getFrozenAmount().add(transaction.getSum()));
                account.setBalance(account.getBalance().subtract(transaction.getSum()));
                accountRepository.save(account);
            }
            case REJECTED -> {
                account.setBalance(account.getBalance().subtract(transaction.getSum()));
                accountRepository.save(account);
            }
        }

        transactionRepository.save(transaction);
        log.info("Обновлена транзакция {} со статусом {}", transactionId, message.getStatus());
    }
}