package resenkov.work.t1transactionlistener.generate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import resenkov.work.t1transactionlistener.config.TransactionCheckProperties;
import resenkov.work.t1transactionlistener.dto.TransactionMessage;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TransactionCheckProperties props;

    @InjectMocks
    private DataInitializer initializer;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    private static final String TOPIC = "t1_demo_transactions";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(props.getMaxTx()).thenReturn(3);

        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString()))
                .thenReturn(future);

        when(objectMapper.writeValueAsString(any(TransactionMessage.class)))
                .thenAnswer(invocation -> {
                    TransactionMessage msg = invocation.getArgument(0);
                    return String.format(
                            "{\"transactionId\":%d,\"accountId\":%d,\"clientId\":%d,\"amount\":%s,\"timestamp\":\"%s\"}",
                            msg.getTransactionId(),
                            msg.getAccountId(),
                            msg.getClientId(),
                            msg.getAmount(),
                            msg.getTimestamp().toString()
                    );
                });
    }

    @Test
    void testCreateAccount() {
        Client client = new Client();
        client.setId(42L);

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> {
                    Account acc = invocation.getArgument(0);
                    acc.setId(77L);
                    return acc;
                });

        Account result = initializer.createAccount(client, Account.Status.OPEN, 123.45);

        assertThat(result.getClient()).isSameAs(client);
        assertThat(result.getStatus()).isEqualTo(Account.Status.OPEN);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(123.45).setScale(2));
        assertThat(result.getBalanceType()).isEqualTo(Account.BalanceType.DEBIT);
        assertThat(result.getFrozenAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getAccountId()).isEqualTo(77L);

        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    void testSendAcceptedTransaction() throws Exception {
        Account acc = new Account();
        acc.setAccountId(10L);
        acc.setBalance(BigDecimal.valueOf(1000));
        Client client = new Client();
        client.setClientId(20L);

        initializer.sendAcceptedTransaction(acc, client);

        verify(kafkaTemplate).send(topicCaptor.capture(), payloadCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC);

        JsonNode node = new ObjectMapper().readTree(payloadCaptor.getValue());
        assertThat(node.get("transactionId").asLong()).isPositive();
        assertThat(node.get("accountId").asLong()).isEqualTo(10L);
        assertThat(node.get("clientId").asLong()).isEqualTo(20L);
        assertThat(new BigDecimal(node.get("amount").asText()))
                .isEqualByComparingTo(BigDecimal.valueOf(100).setScale(2));
    }

    @Test
    void testSendRejectedTransaction() throws Exception {
        Account acc = new Account();
        acc.setAccountId(11L);
        acc.setBalance(BigDecimal.valueOf(50));
        Client client = new Client();
        client.setClientId(21L);

        initializer.sendRejectedTransaction(acc, client);

        verify(kafkaTemplate).send(topicCaptor.capture(), payloadCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC);

        JsonNode node = new ObjectMapper().readTree(payloadCaptor.getValue());
        BigDecimal sent = new BigDecimal(node.get("amount").asText());
        assertThat(sent)
                .isEqualByComparingTo(acc.getBalance().add(BigDecimal.valueOf(-50000)));
    }

    @Test
    void testSendBlockedTransactions() {
        Account acc = new Account();
        acc.setAccountId(12L);
        Client client = new Client();
        client.setClientId(22L);

        initializer.sendBlockedTransactions(acc, client);

        verify(kafkaTemplate, times(props.getMaxTx() + 1))
                .send(eq(TOPIC), anyString());
    }

    @Test
    void testSendIgnoredTransaction() {
        Account acc = new Account();
        acc.setAccountId(13L);
        acc.setStatus(Account.Status.CLOSED);
        Client client = new Client();
        client.setClientId(23L);

        initializer.sendIgnoredTransaction(acc, client);

        verify(kafkaTemplate).send(topicCaptor.capture(), payloadCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC);
    }

    @Test
    void testSendAccountNotFoundTransaction() throws Exception {
        initializer.sendAccountNotFoundTransaction();

        verify(kafkaTemplate).send(topicCaptor.capture(), payloadCaptor.capture());
        assertThat(topicCaptor.getValue()).isEqualTo(TOPIC);

        JsonNode node = new ObjectMapper().readTree(payloadCaptor.getValue());
        assertThat(node.get("accountId").asLong()).isEqualTo(999_999L);
    }
}
