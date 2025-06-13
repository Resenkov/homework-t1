package resenkov.work.t1unlockaccount.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.repository.AccountRepository;
import resenkov.work.t1entity.repository.ClientRepository;
import resenkov.work.t1unlockaccount.UnlockService;
import resenkov.work.t1unlockaccount.config.UnlockDecisionMaker;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("Unit tests for UnlockService")
class UnlockServiceTests {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private UnlockDecisionMaker decisionMaker;

    @InjectMocks
    private UnlockService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("unlockClients unlocks only BLOCKED clients when decisionMaker allows")
    void testUnlockClients_successful() {
        Client blocked1 = new Client(); blocked1.setClientId(1L); blocked1.setStatus(Client.Status.BLOCKED);
        Client active2 = new Client(); active2.setClientId(2L); active2.setStatus(Client.Status.ACTIVE);
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(blocked1));
        when(clientRepository.findById(2L)).thenReturn(Optional.of(active2));
        when(clientRepository.findById(3L)).thenReturn(Optional.empty());
        when(decisionMaker.shouldUnlock()).thenReturn(true);

        List<Long> unlocked = service.unlockClients(ids);

        assertEquals(1, unlocked.size());
        assertTrue(unlocked.contains(1L));
        assertEquals(Client.Status.ACTIVE, blocked1.getStatus());
        verify(clientRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("unlockClients does nothing when decisionMaker denies")
    void testUnlockClients_denied() {
        Client blocked = new Client(); blocked.setClientId(1L); blocked.setStatus(Client.Status.BLOCKED);
        when(clientRepository.findById(1L)).thenReturn(Optional.of(blocked));
        when(decisionMaker.shouldUnlock()).thenReturn(false);

        List<Long> unlocked = service.unlockClients(List.of(1L));

        assertTrue(unlocked.isEmpty());
        assertEquals(Client.Status.BLOCKED, blocked.getStatus());
        verify(clientRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("unlockAccounts unlocks only ARRESTED accounts when decisionMaker allows")
    void testUnlockAccounts_successful() {
        Account arrested = new Account(); arrested.setAccountId(10L); arrested.setStatus(Account.Status.ARRESTED);
        Account open = new Account(); open.setAccountId(11L); open.setStatus(Account.Status.OPEN);

        when(accountRepository.findById(10L)).thenReturn(Optional.of(arrested));
        when(accountRepository.findById(11L)).thenReturn(Optional.of(open));
        when(decisionMaker.shouldUnlock()).thenReturn(true);

        List<Long> unlocked = service.unlockAccounts(Arrays.asList(10L, 11L));

        assertEquals(1, unlocked.size());
        assertTrue(unlocked.contains(10L));
        assertEquals(Account.Status.OPEN, arrested.getStatus());
        verify(accountRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("unlockAccounts does nothing when no ARRESTED or decisionMaker denies")
    void testUnlockAccounts_none() {
        Account arrested = new Account(); arrested.setAccountId(10L); arrested.setStatus(Account.Status.ARRESTED);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(arrested));
        when(decisionMaker.shouldUnlock()).thenReturn(false);

        List<Long> unlocked = service.unlockAccounts(List.of(10L));

        assertTrue(unlocked.isEmpty());
        assertEquals(Account.Status.ARRESTED, arrested.getStatus());
        verify(accountRepository, never()).saveAll(anyList());
    }
}
