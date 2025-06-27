package resenkov.work.t1transactionlistener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.shaded.com.google.protobuf.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.entity.Transaction;
import resenkov.work.t1entity.repository.AccountRepository;
import resenkov.work.t1entity.repository.ClientRepository;
import resenkov.work.t1entity.repository.TransactionRepository;
import resenkov.work.t1transactionlistener.config.WebClientConfig;
import resenkov.work.t1transactionlistener.dto.TransactionMessage;

import javax.net.ssl.SSLException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class TransactionListenerService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ClientRepository clientRepository;
    private final WebClient service2Client;

    private static final String ACCEPT_TOPIC = "t1_demo_transaction_accept";

    @Autowired
    public TransactionListenerService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            ClientRepository clientRepository,
            WebClientConfig webClientConfig) throws SSLException {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.clientRepository = clientRepository;
        this.service2Client = webClientConfig.service2WebClient();
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

        Optional<Client> optionalClient = clientRepository.findByClientId(incomingClientId);
        if (optionalClient.isEmpty()) {
            log.warn("Клиент не найден: {}. Транзакция {} проигнорирована",
                    incomingClientId, incomingTxId);
            return;
        }
        Client client = optionalClient.get();

        if (client.getStatus() == null) {
            String result = service2Client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/check-blacklist")
                            .queryParam("clientId", incomingClientId)
                            .queryParam("accountId", incomingAccountId)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> {
                        return response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Service2 error {}: {}", response.statusCode(), errorBody);
                                    return Mono.error(new ServiceException(
                                            "Service2 error: " + response.statusCode() + " - " + errorBody
                                    ));
                                });
                    })
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .map(map -> map.get("status"))
                    .block();

            if ("BLOCKED".equalsIgnoreCase(result)) {
                client.setStatus(Client.Status.BLOCKED);
                clientRepository.save(client);

                account.setStatus(Account.Status.BLOCKED);
                accountRepository.save(account);

                Transaction trx = new Transaction();
                trx.setTranscationId(incomingTxId);
                trx.setAccount(account);
                trx.setStatus(Transaction.Status.REJECTED);
                trx.setSum(amount);
                transactionRepository.save(trx);

                log.info("Клиент {} и счёт {} помечены как BLOCKED (черный список). Транзакция {} отклонена",
                        incomingClientId, incomingAccountId, incomingTxId);
                return;
            } else {
                client.setStatus(Client.Status.ACTIVE);
                clientRepository.save(client);

                account.setStatus(Account.Status.OPEN);
                accountRepository.save(account);

                log.info("Клиент {} в порядке, устанавливаем статус ACTIVE. Продолжаем обработку транзакции {}",
                        incomingClientId, incomingTxId);
            }
        }

        if (account.getClient().getStatus() == Client.Status.BLOCKED) {
            Transaction transaction = new Transaction();
            transaction.setAccount(account);

            log.info("Клиент {} был уже BLOCKED → транзакция {} сразу REJECTED",
                    incomingClientId, incomingTxId);
            return;
        }

        if (account.getClient().getClientId() == null) {
            log.error("Не удалось получить clientId для аккаунта: {}", incomingAccountId);
            return;
        }

        if (!account.getClient().getClientId().equals(incomingClientId)) {
            log.warn("Счёт {} не принадлежит клиенту {}. Транзакция {} проигнорирована",
                    incomingAccountId, incomingClientId, incomingTxId);
            return;
        }

        if (!Account.Status.OPEN.equals(account.getStatus())) {
            log.warn("Статус счёта {} не OPEN ({}). Транзакция {} проигнорирована",
                    incomingAccountId, account.getStatus(), incomingTxId);
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

    @KafkaListener(
            topics = "t1_demo_transaction_result",
            groupId = "transaction-result-processor",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void updateAfter(TransactionMessage message) {
        Long transactionId = message.getTransactionId();
        Long accountId = message.getAccountId();

        Optional<Transaction> resulttx = Optional.ofNullable(
                transactionRepository.findByTranscationId(transactionId)
        );
        Optional<Account> checkAccount = accountRepository.findByAccountId(accountId);
        if (resulttx.isEmpty() || checkAccount.isEmpty()) {
            log.warn("Транзакция или счёт не найдены: {}", message);
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
