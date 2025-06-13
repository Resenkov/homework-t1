package resenkov.work.t1transactionlistener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resenkov.work.t1entity.entity.Transaction;
import resenkov.work.t1entity.repository.TransactionRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    TransactionRepository repo;

    @InjectMocks
    TransactionService service;

    @Test
    void addTransaction_delegatesToSave() {
        Transaction t = new Transaction();
        when(repo.save(t)).thenReturn(t);
        assertThat(service.addTransaction(t)).isSameAs(t);
        verify(repo).save(t);
    }

    @Test
    void findAll_delegatesToFindAll() {
        List<Transaction> all = Arrays.asList(new Transaction(), new Transaction());
        when(repo.findAll()).thenReturn(all);
        assertThat(service.findAll()).isEqualTo(all);
    }

    @Test
    void updateTransaction_delegatesToSave() {
        Transaction t = new Transaction();
        when(repo.save(t)).thenReturn(t);
        assertThat(service.updateTransaction(t)).isSameAs(t);
        verify(repo).save(t);
    }

    @Test
    void deleteTransaction_delegatesToDeleteById() {
        service.deleteTransaction(99L);
        verify(repo).deleteById(99L);
    }

    @Test
    void getById_delegatesToGetReferenceById() {
        Transaction t = new Transaction();
        when(repo.getReferenceById(55L)).thenReturn(t);
        assertThat(service.getById(55L)).isSameAs(t);
    }
}
