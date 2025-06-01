package resenkov.work.t1business.generate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import resenkov.work.t1business.config.TransactionCheckProperties;
import resenkov.work.t1business.dto.TransactionMessage;
import resenkov.work.t1business.entity.Account;
import resenkov.work.t1business.entity.Client;
import resenkov.work.t1business.repository.AccountRepository;
import resenkov.work.t1business.repository.ClientRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Slf4j
@Component
@Transactional
public class DataInitializer {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionCheckProperties props;

    private static final String INCOMING_TOPIC = "t1_demo_transactions";

    @Autowired
    public DataInitializer(
            ClientRepository clientRepository,
            AccountRepository accountRepository,
            @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> stringKafkaTemplate,
            ObjectMapper objectMapper,
            TransactionCheckProperties props
    ) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.kafkaTemplate = stringKafkaTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    @PostConstruct
    public void initTestData() {
        for (int i = 1; i <= 3; i++) {
            Client client = new Client();
            client.setFirstName("Клиент" + i);
            client.setLastName("Тестович");
            client.setMiddleName("Демо");
            client = clientRepository.save(client);
            client.setClientId(client.getId());
            clientRepository.save(client);

            // ACCEPTED
            Account accAccepted = createAccount(client, Account.Status.OPEN, 1000);
            sendAcceptedTransaction(accAccepted, client);

            // REJECTED
            Account accRejected = createAccount(client, Account.Status.OPEN, 20);
            sendRejectedTransaction(accRejected, client);

            // BLOCKED
            Account accBlocked = createAccount(client, Account.Status.OPEN, 0);
            sendBlockedTransactions(accBlocked, client);

            // IGNORED
            Account accClosed = createAccount(client, Account.Status.CLOSED, 500);
            sendIgnoredTransaction(accClosed, client);
        }

        // Несуществующий счёт
        sendAccountNotFoundTransaction();
    }

    private Account createAccount(Client client, Account.Status status, double balance) {
        Account acc = new Account();
        acc.setClient(client);
        acc.setStatus(status);
        acc.setBalanceType(Account.BalanceType.DEBIT);
        acc.setBalance(BigDecimal.valueOf(balance).setScale(2));
        acc.setFrozenAmount(BigDecimal.ZERO);
        acc = accountRepository.save(acc);
        acc.setAccountId(acc.getId());
        return accountRepository.save(acc);
    }


    /**
     * 1) Отправка одной транзакции, которая будет принята (ACCEPTED).
     */
    private void sendAcceptedTransaction(Account account, Client client) {
        Long txId = System.currentTimeMillis() + account.getAccountId() * 10;
        BigDecimal amount = BigDecimal.valueOf(100).setScale(2, BigDecimal.ROUND_HALF_UP);
        log.info("=== Отправляем ACCEPTED транзакцию: txId={}, accountId={}, clientId={}, amount={}",
                txId, account.getAccountId(), client.getClientId(), amount);
        sendTransactionMessage(txId, account.getAccountId(), client.getClientId(), amount);
    }

    /**
     * 2) Отправка одной транзакции, сумма которой превышает баланс — будет REJECTED.
     */
    private void sendRejectedTransaction(Account account, Client client) {
        Long txId = System.currentTimeMillis() + account.getAccountId() * 20;
        BigDecimal amount = account.getBalance().add(BigDecimal.valueOf(-50000)); // Явное сильное превышение
        log.info("=== Отправляем REJECTED транзакцию: txId={}, accountId={}, clientId={}, amount={}",
                txId, account.getAccountId(), client.getClientId(), amount);
        sendTransactionMessage(txId, account.getAccountId(), client.getClientId(), amount);
    }


    /**
     * 3) Отправка нескольких транзакций подряд по одному и тому же счету,
     * чтобы превысить допустимое количество (props.getMaxTx()) и получить BLOCKED.
     * Все суммы будут небольшими, чтобы баланс не стал отрицательным раньше.
     */
    private void sendBlockedTransactions(Account account, Client client) {
        int maxTx = props.getMaxTx();
        log.info("=== Отправляем {}+1 транзакций, чтобы получить BLOCKED (accountId={}, clientId={})", maxTx, account.getAccountId(), client.getClientId());
        for (int i = 0; i < maxTx + 1; i++) {
            Long txId = System.currentTimeMillis() + account.getAccountId() * 100 + i;
            BigDecimal smallAmount = BigDecimal.valueOf(1).setScale(2, BigDecimal.ROUND_HALF_UP);
            sendTransactionMessage(txId, account.getAccountId(), client.getClientId(), smallAmount);
            sleepMillis(100);
        }
    }

    /**
     * 4) Отправка транзакции по закрытому счету — будет проигнорировано (Account not OPEN).
     */
    private void sendIgnoredTransaction(Account account, Client client) {
        Long txId = System.currentTimeMillis() + account.getAccountId() * 30;
        BigDecimal amount = BigDecimal.valueOf(50).setScale(2, BigDecimal.ROUND_HALF_UP);
        log.info("=== Отправляем IGNORED транзакцию (счет CLOSED): txId={}, accountId={}, clientId={}, amount={}",
                txId, account.getAccountId(), client.getClientId(), amount);
        sendTransactionMessage(txId, account.getAccountId(), client.getClientId(), amount);
    }

    /**
     * 5) Отправка транзакции по несуществующему счету — будет игнор (Account not found).
     */
    private void sendAccountNotFoundTransaction() {
        Long fakeAccountId = 999_999L;
        Long txId = System.currentTimeMillis() + fakeAccountId;
        BigDecimal amount = BigDecimal.valueOf(10).setScale(2, BigDecimal.ROUND_HALF_UP);
        log.info("=== Отправляем транзакцию с несуществующим accountId={}, txId={}", fakeAccountId, txId);
        sendTransactionMessage(txId, fakeAccountId, 1L, amount);
    }

    private void sendTransactionMessage(Long txId, Long accountId, Long clientId, BigDecimal amount) {
        TransactionMessage msg = new TransactionMessage(
                txId, accountId, clientId, amount, LocalDateTime.now()
        );
        try {
            String json = objectMapper.writeValueAsString(msg);
            kafkaTemplate.send(INCOMING_TOPIC, json);
            log.info("Отправлено в {}: {}", INCOMING_TOPIC, json);
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации TransactionMessage", e);
        }
    }

    private void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }
}
