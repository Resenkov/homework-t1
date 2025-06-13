package resenkov.work.t1transactionlistener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.repository.AccountRepository;
import resenkov.work.t1entity.repository.ClientRepository;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Unit tests for BlockedEntitiesService")
class BlockedEntitiesServiceTests {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BlockedEntitiesService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("findBlockedClients returns list and calls repository with correct status and pagination")
    void testFindBlockedClients() {
        // prepare
        Client c1 = new Client(); c1.setClientId(1L); c1.setStatus(Client.Status.BLOCKED);
        Client c2 = new Client(); c2.setClientId(2L); c2.setStatus(Client.Status.BLOCKED);
        List<Client> expected = Arrays.asList(c1, c2);
        int limit = 5;

        when(clientRepository.findByStatus(eq(Client.Status.BLOCKED), eq(PageRequest.of(0, limit))))
                .thenReturn(expected);

        // execute
        List<Client> result = service.findBlockedClients(limit);

        // verify
        assertSame(expected, result);
        verify(clientRepository).findByStatus(Client.Status.BLOCKED, PageRequest.of(0, limit));
    }

    @Test
    @DisplayName("findArrestedAccounts returns list and calls repository with correct status and pagination")
    void testFindArrestedAccounts() {
        // prepare
        Account a1 = new Account(); a1.setAccountId(10L); a1.setStatus(Account.Status.ARRESTED);
        Account a2 = new Account(); a2.setAccountId(20L); a2.setStatus(Account.Status.ARRESTED);
        List<Account> expected = Arrays.asList(a1, a2);
        int limit = 3;

        when(accountRepository.findByStatus(eq(Account.Status.ARRESTED), eq(PageRequest.of(0, limit))))
                .thenReturn(expected);

        // execute
        List<Account> result = service.findArrestedAccounts(limit);

        // verify
        assertSame(expected, result);
        verify(accountRepository).findByStatus(Account.Status.ARRESTED, PageRequest.of(0, limit));
    }
}
