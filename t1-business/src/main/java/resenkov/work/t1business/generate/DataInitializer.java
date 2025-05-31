package resenkov.work.t1business.generate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import resenkov.work.t1business.dto.TransactionMessage;
import resenkov.work.t1business.entity.Account;
import resenkov.work.t1business.entity.Client;
import resenkov.work.t1business.repository.AccountRepository;
import resenkov.work.t1business.repository.ClientRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DataInitializer теперь выполняет два шага:
 * 1) Заполняет БД тестовыми клиентами и счетами (в том числе с статусом OPEN).
 * 2) Генерирует несколько входящих транзакций (TransactionMessage) и отправляет их в Kafka-топик t1_demo_transactions.
 */
@Component
public class DataInitializer {

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Random random;

    // Название топика для входящих транзакций
    private static final String INCOMING_TOPIC = "t1_demo_transactions";

    @Autowired
    public DataInitializer(ClientRepository clientRepository,
                           AccountRepository accountRepository,
                           @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> stringKafkaTemplate, KafkaTemplate<String, String> stringKafkaTemplate1,
                           ObjectMapper objectMapper) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.kafkaTemplate = stringKafkaTemplate;
        this.objectMapper = objectMapper;
        this.random = new Random();
    }

    @PostConstruct
    @Transactional
    public void initTestData() {
        List<Client> clients = createAndSaveClients();
        List<Account> allAccounts = createAndSaveAccountsForClients(clients);

        List<Account> openAccounts = allAccounts.stream()
                .filter(a -> Account.Status.OPEN.equals(a.getStatus()))
                .collect(Collectors.toList());

        if (!openAccounts.isEmpty()) {
            sendTestTransactionsToKafka(openAccounts, /*количество тестовых транзакций на счёт*/ 2);
        }
    }

    // --------------------- 1. Создание клиентов и счетов ---------------------

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
            client.setClientId(null); // заполним после save()

            clientRepository.save(client);
            // После save() автоматически заполнится поле client.id
            client.setClientId(client.getId());
            clientRepository.save(client);

            clients.add(client);
        }
        return clients;
    }

    private List<Account> createAndSaveAccountsForClients(List<Client> clients) {
        List<Account> allAccounts = new ArrayList<>();

        for (Client client : clients) {
            // создаём 1 счёт со статусом OPEN
            Account accountOpen = new Account();
            accountOpen.setClient(client);
            accountOpen.setStatus(Account.Status.OPEN);
            accountOpen.setAccountId(null); // заполним после save()
            accountOpen.setBalanceType(randomBalanceType());
            accountOpen.setBalance(randomInitialBalance());
            accountOpen.setFrozenAmount(BigDecimal.ZERO);

            accountRepository.save(accountOpen);
            accountOpen.setAccountId(accountOpen.getId());
            accountRepository.save(accountOpen);
            allAccounts.add(accountOpen);

            // создаём 1 «дополнительный» счёт с любым статусом, кроме OPEN
            Account accountNonOpen = new Account();
            accountNonOpen.setClient(client);
            accountNonOpen.setStatus(randomNonOpenStatus());
            accountNonOpen.setAccountId(null);
            accountNonOpen.setBalanceType(randomBalanceType());
            accountNonOpen.setBalance(randomInitialBalance());
            accountNonOpen.setFrozenAmount(BigDecimal.ZERO);

            accountRepository.save(accountNonOpen);
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
        Account.Status[] all = Account.Status.values();
        List<Account.Status> nonOpen = new ArrayList<>();
        for (Account.Status s : all) {
            if (s != Account.Status.OPEN) {
                nonOpen.add(s);
            }
        }
        return nonOpen.get(random.nextInt(nonOpen.size()));
    }

    private void sendTestTransactionsToKafka(List<Account> openAccounts, int txPerAccount) {
        long baseTxId = System.currentTimeMillis(); // базовый номер для transactionId, чтобы не дублировать

        for (Account account : openAccounts) {
            Long accountId = account.getAccountId();
            Long clientId = account.getClient().getClientId();

            for (int i = 0; i < txPerAccount; i++) {
                BigDecimal amount = randomTransactionAmount();

                Long transactionId = baseTxId + accountId * 100 + i;

                TransactionMessage msg = new TransactionMessage(
                        transactionId,
                        accountId,
                        clientId,
                        amount,
                        LocalDateTime.now()
                );

                // Сериализуем в JSON и отправляем в Kafka
                try {
                    String json = objectMapper.writeValueAsString(msg);
                    kafkaTemplate.send(INCOMING_TOPIC, json);
                    System.out.printf("Отправлена тестовая транзакция в Kafka: %s%n", json);
                } catch (JsonProcessingException e) {
                    // В реальном приложении логгируйте через Logger, а не printStackTrace
                    e.printStackTrace();
                }
            }
        }
    }

    private BigDecimal randomTransactionAmount() {
        // генерируем сумму случайно от 10 до 10 000, с копейками
        double amt = 10 + random.nextDouble() * (10_000 - 10);
        return BigDecimal.valueOf(amt).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
