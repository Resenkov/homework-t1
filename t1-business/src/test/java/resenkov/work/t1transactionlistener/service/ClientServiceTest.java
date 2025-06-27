package resenkov.work.t1transactionlistener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.repository.ClientRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    ClientRepository repo;

    @InjectMocks
    ClientService service;

    @Test
    void getClient_delegatesToFindById() {
        Client c = new Client();
        when(repo.findById(5L)).thenReturn(Optional.of(c));

        Optional<Client> opt = service.getClient(5L);
        assertThat(opt).contains(c);
        verify(repo).findById(5L);
    }
}
