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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@Transactional
public class DataInitializer {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionCheckProperties props;
    private final Random random;

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
        this.random = new Random();
    }

    @PostConstruct
    public void initTestData() {
        // 1) Создаем клиентов и счета
        List<Client> clients = createAndSaveClients();
        List<Account> allAccounts = createAndSaveAccountsForClients(clients);

        // 2) Фильтруем только открытые счета
        List<Account> openAccounts = allAccounts.stream()
                .filter(a -> Account.Status.OPEN.equals(a.getStatus()))
                .collect(Collectors.toList());

        if (!openAccounts.isEmpty()) {
            sendTestTransactionsToKafka(openAccounts);
        }
    }

    private List<Client> createAndSaveClients() {
        List<Client> clients = new ArrayList<>();
        String[] firstNames = { "Иван", "Мария", "Пётр", "Елена", "Алексей", "Ольга" };
        String[] lastNames  = { "Иванов", "Петрова", "Сидоров", "Смирнова", "Кузнецов", "Попова" };
        String[] middleNames = { "Александрович", "Сергеевна", "Николаевич", "Павловна", "Игоревич", "Дмитриевна" };

        for (int i = 0; i < firstNames.length; i++) {
            Client client = new Client();
            client.setFirstName(firstNames[i]);
            client.setLastName(lastNames[i]);
            client.setMiddleName(middleNames[i]);

            client = clientRepository.save(client);

            client.setClientId(client.getId());
            client = clientRepository.save(client);

            clients.add(client);
        }
        return clients;
    }

    private List<Account> createAndSaveAccountsForClients(List<Client> clients) {
        List<Account> allAccounts = new ArrayList<>();
        for (Client client : clients) {
            // один счет OPEN
            Account accountOpen = new Account();
            accountOpen.setClient(client);
            accountOpen.setStatus(Account.Status.OPEN);
            accountOpen.setBalanceType(randomBalanceType());
            accountOpen.setBalance(randomInitialBalance());
            accountOpen.setFrozenAmount(BigDecimal.ZERO);
            accountOpen = accountRepository.save(accountOpen);
            accountOpen.setAccountId(accountOpen.getId());
            accountRepository.save(accountOpen);
            allAccounts.add(accountOpen);

            // один счет НЕ OPEN
            Account accountNonOpen = new Account();
            accountNonOpen.setClient(client);
            accountNonOpen.setStatus(randomNonOpenStatus());
            accountNonOpen.setBalanceType(randomBalanceType());
            accountNonOpen.setBalance(randomInitialBalance());
            accountNonOpen.setFrozenAmount(BigDecimal.ZERO);
            accountNonOpen = accountRepository.save(accountNonOpen);
            accountNonOpen.setAccountId(accountNonOpen.getId());
            accountRepository.save(accountNonOpen);
            allAccounts.add(accountNonOpen);
        }
        return allAccounts;
    }

    private Account.BalanceType randomBalanceType() {
        Account.BalanceType[] types = Account.BalanceType.values();
        return types[random.nextInt(types.length)];
    }

    private BigDecimal randomInitialBalance() {
        double amount = random.nextDouble() * 100_000;
        return BigDecimal.valueOf(amount).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private Account.Status randomNonOpenStatus() {
        List<Account.Status> nonOpen = new ArrayList<>();
        for (Account.Status s : Account.Status.values()) {
            if (s != Account.Status.OPEN) {
                nonOpen.add(s);
            }
        }
        return nonOpen.get(random.nextInt(nonOpen.size()));
    }

    private void sendTestTransactionsToKafka(List<Account> openAccounts) {
        long baseTxId = System.currentTimeMillis();

        for (Account account : openAccounts) {
            Long accountId = account.getAccountId();
            Long clientId = account.getClient().getClientId();

            // 1) N+1 мелких транзакций, чтобы Service2 заблокировал последние N
            int maxTx = props.getMaxTx();
            for (int i = 0; i < maxTx + 1; i++) {
                BigDecimal smallAmount = BigDecimal.valueOf(10 + random.nextDouble() * 90).setScale(2, BigDecimal.ROUND_HALF_UP);
                Long txId = baseTxId + accountId * 100 + i;
                sendTransactionMessage(txId, accountId, clientId, smallAmount);
                sleepMillis(200); // небольшая пауза, чтобы timestamps отличались
            }

            // 2) Транзакция с amount > balance для REJECTED
            BigDecimal tooBig = account.getBalance().add(BigDecimal.valueOf(1_000));
            long txIdReject = baseTxId + accountId * 100 + maxTx + 10;
            sendTransactionMessage(txIdReject, accountId, clientId, tooBig);

            // 3) Несколько обычных транзакций для ACCEPTED (баланс уменьшится)
            for (int i = 0; i < 2; i++) {
                BigDecimal okAmount = BigDecimal.valueOf(1 + random.nextDouble() * 50).setScale(2, BigDecimal.ROUND_HALF_UP);
                Long txIdOk = baseTxId + accountId * 100 + maxTx + 20 + i;
                sendTransactionMessage(txIdOk, accountId, clientId, okAmount);
                sleepMillis(200);
            }
        }
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
        } catch (InterruptedException ignored) {}
    }

    private BigDecimal randomTransactionAmount() {
        double amt = 10 + random.nextDouble() * (10_000 - 10);
        return BigDecimal.valueOf(amt).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}