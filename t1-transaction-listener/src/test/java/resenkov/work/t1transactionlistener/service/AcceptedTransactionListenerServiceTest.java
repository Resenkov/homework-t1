package resenkov.work.t1transactionlistener.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptedTransactionListenerServiceTest {

    private static final String RESULT_TOPIC = "t1_demo_transaction_result";

    @Mock
    AccountRepository accountRepo;
    @Mock
    TransactionRepository txRepo;
    @Mock
    ClientRepository clientRepo;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    TransactionCheckProperties props;

    @InjectMocks
    AcceptedTransactionListenerService service;

    private TransactionMessage baseMsg;
    private Account account;
    private Client client;

    @BeforeEach
    void setUp() {
        baseMsg = new TransactionMessage();
        baseMsg.setTransactionId(100L);
        baseMsg.setAccountId(10L);
        baseMsg.setClientId(20L);
        baseMsg.setAmount(BigDecimal.valueOf(50));
        baseMsg.setTimestamp(LocalDateTime.now());

        client = new Client();
        client.setId(20L);
        account = new Account();
        account.setAccountId(10L);
        account.setId(1L);
        account.setBalance(BigDecimal.valueOf(100));
        account.setClient(client);

        // Только если используешь в каждом тесте
        lenient().when(props.getRejectedThreshold()).thenReturn(5);
        lenient().when(props.getWindowSeconds()).thenReturn(60L);
        lenient().when(props.getMaxTx()).thenReturn(3);
    }


    @Test
    void whenNoExistingRequestedTx_thenOnlyFindByTranscationId() {
        when(txRepo.findByTranscationId(100L)).thenReturn(null);

        service.listen(baseMsg);

        verify(txRepo).findByTranscationId(100L);
        verifyNoMoreInteractions(txRepo, accountRepo, clientRepo, kafkaTemplate);
    }

    @Test
    void whenAccountNotFound_thenException() {
        when(txRepo.findByTranscationId(100L)).thenReturn(new Transaction());
        when(accountRepo.findByAccountId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listen(baseMsg))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Account not found");

        verify(accountRepo).findByAccountId(10L);
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void whenClientMismatch_thenNoKafkaSend() {
        when(txRepo.findByTranscationId(100L)).thenReturn(new Transaction());
        when(accountRepo.findByAccountId(10L)).thenReturn(Optional.of(account));
        // вернём клиента с другим id
        Client other = new Client();
        other.setId(999L);
        when(clientRepo.findByClientId(20L)).thenReturn(Optional.of(other));

        service.listen(baseMsg);

        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void whenRejectedThresholdExceeded_thenArrestAndDbSave() {
        when(txRepo.findByTranscationId(100L)).thenReturn(new Transaction());
        when(accountRepo.findByAccountId(10L)).thenReturn(Optional.of(account));
        when(clientRepo.findByClientId(20L)).thenReturn(Optional.of(client));
        when(txRepo.countByAccountAndStatus(account, Transaction.Status.REJECTED))
                .thenReturn(5);

        service.listen(baseMsg);

        assertThat(account.getStatus()).isEqualTo(Account.Status.ARRESTED);
        verify(accountRepo).save(account);
        verify(txRepo).save(argThat(t ->
                t.getTranscationId().equals(100L) &&
                        t.getStatus() == Transaction.Status.REJECTED
        ));
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void whenBelowThresholdAndBelowMaxTx_thenSendAccepted() throws JsonProcessingException {
        when(txRepo.findByTranscationId(100L)).thenReturn(new Transaction());
        when(accountRepo.findByAccountId(10L)).thenReturn(Optional.of(account));
        when(clientRepo.findByClientId(20L)).thenReturn(Optional.of(client));
        when(txRepo.countByAccountAndStatus(account, Transaction.Status.REJECTED)).thenReturn(0);
        when(txRepo.countByAccountAndCreatedAtBetween(
                eq(account), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2);
        when(objectMapper.writeValueAsString(any(TransactionMessage.class)))
                .thenReturn("json-payload");

        service.listen(baseMsg);

        // в итоговом сообщении статус ACCEPTED
        verify(objectMapper).writeValueAsString(argThat(o -> {
            TransactionMessage m = (TransactionMessage) o;
            return m.getStatus() == Transaction.Status.ACCEPTED;
        }));
        verify(kafkaTemplate).send(RESULT_TOPIC, "json-payload");
    }

    @Test
    void whenNewBalanceNegative_thenSendRejected() throws JsonProcessingException {
        when(txRepo.findByTranscationId(100L)).thenReturn(new Transaction());
        when(accountRepo.findByAccountId(10L)).thenReturn(Optional.of(account));
        when(clientRepo.findByClientId(20L)).thenReturn(Optional.of(client));
        when(txRepo.countByAccountAndStatus(account, Transaction.Status.REJECTED)).thenReturn(0);
        when(txRepo.countByAccountAndCreatedAtBetween(
                eq(account), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);
        when(objectMapper.writeValueAsString(any(TransactionMessage.class)))
                .thenReturn("json-payload");
        // отрицательный итоговый баланс
        baseMsg.setAmount(BigDecimal.valueOf(-500));

        service.listen(baseMsg);

        verify(objectMapper).writeValueAsString(argThat(o -> {
            TransactionMessage m = (TransactionMessage) o;
            return m.getStatus() == Transaction.Status.REJECTED;
        }));
        verify(kafkaTemplate).send(RESULT_TOPIC, "json-payload");
    }

    @Test
    void whenReachMaxTx_thenSendBlocked() throws JsonProcessingException {
        Transaction existingTx = new Transaction();
        existingTx.setTranscationId(100L);
        existingTx.setStatus(Transaction.Status.REQUESTED);
        existingTx.setAccount(account);
        existingTx.setCreatedAt(LocalDateTime.now().minusSeconds(10));

        when(txRepo.findByTranscationId(100L)).thenReturn(existingTx);
        when(accountRepo.findByAccountId(10L)).thenReturn(Optional.of(account));
        when(clientRepo.findByClientId(20L)).thenReturn(Optional.of(client));
        when(txRepo.countByAccountAndStatus(account, Transaction.Status.REJECTED)).thenReturn(0);
        when(txRepo.countByAccountAndCreatedAtBetween(eq(account), any(), any()))
                .thenReturn(3);
        when(objectMapper.writeValueAsString(any(TransactionMessage.class)))
                .thenReturn("json-payload");

        service.listen(baseMsg);

        verify(objectMapper).writeValueAsString(argThat(o -> {
            TransactionMessage m = (TransactionMessage) o;
            return m.getStatus() == Transaction.Status.BLOCKED;
        }));
        verify(kafkaTemplate).send(RESULT_TOPIC, "json-payload");
    }
}