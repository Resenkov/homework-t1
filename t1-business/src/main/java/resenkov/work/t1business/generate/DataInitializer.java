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
import resenkov.work.t1business.aop.LogDataError;
import resenkov.work.t1business.aop.Metric;
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
    private final Random random;

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

    @LogDataError
    @Metric
    @PostConstruct
    public void initTestData() {
        List<Client> clients = createAndSaveClients();
        List<Account> allAccounts = createAndSaveAccountsForClients(clients);

        List<Account> openAccounts = allAccounts.stream()
                .filter(a -> Account.Status.OPEN.equals(a.getStatus()))
                .collect(Collectors.toList());

        if (!openAccounts.isEmpty()) {
            sendTestTransactionsToKafka(openAccounts, 2);
        }
    }

    @LogDataError
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
            client.setClientId(null);

            clientRepository.save(client);
            client.setClientId(client.getId());
            clientRepository.save(client);

            clients.add(client);
        }
        return clients;
    }

    @LogDataError
    @Metric
    private List<Account> createAndSaveAccountsForClients(List<Client> clients) {
        List<Account> allAccounts = new ArrayList<>();

        for (Client client : clients) {
            Account accountOpen = new Account();
            accountOpen.setClient(client);
            accountOpen.setStatus(Account.Status.OPEN);
            accountOpen.setAccountId(null);
            accountOpen.setBalanceType(randomBalanceType());
            accountOpen.setBalance(randomInitialBalance());
            accountOpen.setFrozenAmount(BigDecimal.ZERO);

            accountRepository.save(accountOpen);
            accountOpen.setAccountId(accountOpen.getId());
            accountRepository.save(accountOpen);
            allAccounts.add(accountOpen);

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

    @LogDataError
    private void sendTestTransactionsToKafka(List<Account> openAccounts, int txPerAccount) {
        long baseTxId = System.currentTimeMillis();
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

                try {
                    String json = objectMapper.writeValueAsString(msg);
                    kafkaTemplate.send(INCOMING_TOPIC, json);
                    log.info("Отправлено сообщение в топик " + INCOMING_TOPIC + "{}", json);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private BigDecimal randomTransactionAmount() {
        double amt = 10 + random.nextDouble() * (10_000 - 10);
        return BigDecimal.valueOf(amt).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
