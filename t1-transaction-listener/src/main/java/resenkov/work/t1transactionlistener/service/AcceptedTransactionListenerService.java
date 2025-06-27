package resenkov.work.t1transactionlistener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.entity.Transaction;
import resenkov.work.t1entity.repository.AccountRepository;
import resenkov.work.t1entity.repository.ClientRepository;
import resenkov.work.t1entity.repository.TransactionRepository;
import resenkov.work.t1transactionlistener.config.TransactionCheckProperties;
import resenkov.work.t1transactionlistener.dto.TransactionMessage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;


@Service
@Log4j2
@RequiredArgsConstructor
public class AcceptedTransactionListenerService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ClientRepository clientRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionCheckProperties properties;

    private static final String RESULT_TOPIC = "t1_demo_transaction_result";

    @KafkaListener(
            topics = "t1_demo_transaction_accept",
            groupId = "transaction-processor",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(TransactionMessage message) {
        Long accountId     = message.getAccountId();
        Long clientId      = message.getClientId();
        Long transactionId = message.getTransactionId();
        LocalDateTime messageTime = message.getTimestamp();

        Optional<Transaction> optExisting =
                Optional.ofNullable(transactionRepository.findByTranscationId(transactionId));

        if (optExisting.isEmpty()) {
            log.error("Ожидалось, что транзакция {} уже будет в БД (REQUESTED), но не нашлось.", transactionId);
            return;
        }

        Account account = accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    log.error("Аккаунт не найден: {}. Транзакция {} игнорируется.", accountId, transactionId);
                    return new IllegalStateException("Account not found");
                });

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> {
                    log.error("Клиент не найден: {}. Транзакция {} игнорируется.", clientId, transactionId);
                    return new IllegalStateException("Client not found");
                });

        if (!account.getClient().getId().equals(client.getId())) {
            log.error("Аккаунт {} (БД ID {}) не принадлежит client {} (DB ID {}). Транзакция {} игнорируется",
                    accountId, account.getId(), clientId, client.getId(), transactionId);
            return;
        }

        int rejectedCount = transactionRepository.countByAccountAndStatus(account, Transaction.Status.REJECTED);
        if (rejectedCount >= properties.getRejectedThreshold()) {
            account.setStatus(Account.Status.ARRESTED);
            accountRepository.save(account);

            Transaction trx = new Transaction();
            trx.setTranscationId(transactionId);
            trx.setAccount(account);
            trx.setStatus(Transaction.Status.REJECTED);
            trx.setSum(message.getAmount());
            trx.setCreatedAt(messageTime);
            transactionRepository.save(trx);
            return;
        }

        LocalDateTime windowStart = messageTime.minusSeconds(properties.getWindowSeconds());
        int txCount = transactionRepository.countByAccountAndCreatedAtBetween(
                account, windowStart, messageTime);

        Transaction.Status newStatus = (txCount >= properties.getMaxTx())
                ? Transaction.Status.BLOCKED
                : Transaction.Status.ACCEPTED;

        BigDecimal newBalance = account.getBalance().add(message.getAmount());
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newStatus = Transaction.Status.REJECTED;
        }
        TransactionMessage resultMessage = createResultMessage(message, newStatus);
        sendNotification(resultMessage);
    }


    public TransactionMessage createResultMessage(TransactionMessage original, Transaction.Status status) {
        TransactionMessage result = new TransactionMessage();
        result.setTransactionId(original.getTransactionId());
        result.setAccountId(original.getAccountId());
        result.setClientId(original.getClientId());
        result.setAmount(original.getAmount());
        result.setStatus(status);
        result.setTimestamp(original.getTimestamp());
        return result;
    }

    public void sendNotification(TransactionMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(RESULT_TOPIC, payload);
            log.info("Сообщение успешно отправлено: {}", payload);
        } catch (JsonProcessingException e) {
            log.error("Ошибка при сереализации сообщения: {}", message.getTransactionId(), e);
        }
    }
}