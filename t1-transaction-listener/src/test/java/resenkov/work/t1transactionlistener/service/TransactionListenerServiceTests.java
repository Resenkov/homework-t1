package resenkov.work.t1transactionlistener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;
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
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Combined unit and integration tests for TransactionListenerService
 */
public class TransactionListenerServiceTests {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private KafkaTemplate<String, String> kafkaTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private WebClient service2Client;
    @Mock private WebClientConfig webClientConfig;

    @InjectMocks private TransactionListenerService service;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        when(webClientConfig.service2WebClient()).thenReturn(service2Client);
    }

    @Nested
    @DisplayName("Unit tests for updateAfter method")
    class UpdateAfterUnitTests {

        @Test
        void testUpdateAfterAccepted() {
            Long txId = 1L;
            Long accId = 2L;
            TransactionMessage msg = new TransactionMessage(txId, accId, null, null, Transaction.Status.ACCEPTED, null);
            Transaction tx = new Transaction();
            tx.setTranscationId(txId);
            tx.setSum(new BigDecimal("100"));
            Account acc = new Account();
            acc.setAccountId(accId);
            acc.setBalance(new BigDecimal("1000"));
            acc.setFrozenAmount(BigDecimal.ZERO);

            when(transactionRepository.findByTranscationId(txId)).thenReturn(tx);
            when(accountRepository.findByAccountId(accId)).thenReturn(Optional.of(acc));

            service.updateAfter(msg);

            verify(transactionRepository).save(tx);
            assertEquals(new BigDecimal("1000"), acc.getBalance());
            assertEquals(BigDecimal.ZERO, acc.getFrozenAmount());
        }

        @Test
        void testUpdateAfterBlocked() {
            Long txId = 1L;
            Long accId = 2L;
            TransactionMessage msg = new TransactionMessage(txId, accId, null, null, Transaction.Status.BLOCKED, null);
            Transaction tx = new Transaction();
            tx.setTranscationId(txId);
            tx.setSum(new BigDecimal("100"));
            Account acc = new Account();
            acc.setAccountId(accId);
            acc.setBalance(new BigDecimal("1000"));
            acc.setFrozenAmount(BigDecimal.ZERO);

            when(transactionRepository.findByTranscationId(txId)).thenReturn(tx);
            when(accountRepository.findByAccountId(accId)).thenReturn(Optional.of(acc));

            service.updateAfter(msg);

            verify(accountRepository).save(acc);
            assertEquals(new BigDecimal("900"), acc.getBalance());
            assertEquals(new BigDecimal("100"), acc.getFrozenAmount());
        }

        @Test
        void testUpdateAfterRejected() {
            Long txId = 1L;
            Long accId = 2L;
            TransactionMessage msg = new TransactionMessage(txId, accId, null, null, Transaction.Status.REJECTED, null);
            Transaction tx = new Transaction();
            tx.setTranscationId(txId);
            tx.setSum(new BigDecimal("100"));
            Account acc = new Account();
            acc.setAccountId(accId);
            acc.setBalance(new BigDecimal("1000"));
            acc.setFrozenAmount(BigDecimal.ZERO);

            when(transactionRepository.findByTranscationId(txId)).thenReturn(tx);
            when(accountRepository.findByAccountId(accId)).thenReturn(Optional.of(acc));

            service.updateAfter(msg);

            verify(accountRepository).save(acc);
            assertEquals(new BigDecimal("900"), acc.getBalance());
        }
    }

    @Nested
    @DisplayName("Integration tests for blacklist check via WireMock")
    class ListenIntegrationTests {
        private static com.github.tomakehurst.wiremock.WireMockServer wireMock;

        @BeforeAll
        static void startWireMock() {
            wireMock = new com.github.tomakehurst.wiremock.WireMockServer(9561);
            wireMock.start();
        }

        @AfterAll
        static void stopWireMock() {
            wireMock.stop();
        }

        @BeforeEach
        void initClient() throws SSLException {
            // переподключаем реальный WebClient к WireMock
            WebClient realClient = WebClient.builder()
                    .baseUrl("http://localhost:9561")
                    .build();
            when(webClientConfig.service2WebClient()).thenReturn(realClient);
            service = new TransactionListenerService(
                    accountRepository,
                    transactionRepository,
                    kafkaTemplate,
                    objectMapper,
                    clientRepository,
                    new resenkov.work.t1transactionlistener.config.WebClientConfig() {
                        @Override
                        public WebClient service2WebClient() {
                            return realClient;
                        }
                    }
            );
        }

        @Test
        void testListen_whenClientStatusNull_blockedResponse() throws JsonProcessingException {
            wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(
                            com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/check-blacklist"))
                    .withQueryParam("clientId", equalTo("5"))
                    .withQueryParam("accountId", equalTo("10"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"status\": \"BLOCKED\" }")));

            Account acc = new Account(); acc.setAccountId(10L); acc.setClient(new Client());
            Client client = new Client(); client.setClientId(5L); client.setStatus(null);
            when(accountRepository.findByAccountId(10L)).thenReturn(Optional.of(acc));
            when(clientRepository.findByClientId(5L)).thenReturn(Optional.of(client));

            TransactionMessage msg = new TransactionMessage(100L, 10L, 5L, new BigDecimal("50"), null, LocalDateTime.now());
            service.listen(msg);

            assertEquals(Client.Status.BLOCKED, client.getStatus());
            assertEquals(Account.Status.BLOCKED, acc.getStatus());
            verify(transactionRepository).save(any(Transaction.class));
        }

        @Test
        void testListen_whenClientStatusNull_activeResponse() {
            wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(
                            com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo("/check-blacklist"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{ \"status\": \"ACTIVE\" }")));

            Account acc = new Account(); acc.setAccountId(20L); acc.setClient(new Client());
            Client client = new Client(); client.setClientId(6L); client.setStatus(null);
            when(accountRepository.findByAccountId(20L)).thenReturn(Optional.of(acc));
            when(clientRepository.findByClientId(6L)).thenReturn(Optional.of(client));

                    TransactionMessage msg = new TransactionMessage(101L, 20L, 6L, new BigDecimal("75"), null, LocalDateTime.now());
            service.listen(msg);

            assertEquals(Client.Status.ACTIVE, client.getStatus());
            assertEquals(Account.Status.OPEN, acc.getStatus());
            verify(transactionRepository, never()).save(argThat(t -> t.getStatus() == Transaction.Status.REJECTED));
        }
    }
}
