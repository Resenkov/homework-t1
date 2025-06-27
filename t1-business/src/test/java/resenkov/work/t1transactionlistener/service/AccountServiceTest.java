package resenkov.work.t1transactionlistener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.repository.AccountRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository repo;

    @InjectMocks
    AccountService service;

    @Test
    void addAccount_delegatesToSave() {
        Account acc = new Account();
        when(repo.save(acc)).thenReturn(acc);
        Account result = service.addAccount(acc);
        assertThat(result).isSameAs(acc);
        verify(repo).save(acc);
    }

    @Test
    void updateAccount_delegatesToSave() {
        Account acc = new Account();
        when(repo.save(acc)).thenReturn(acc);
        Account result = service.updateAccount(acc);
        assertThat(result).isSameAs(acc);
        verify(repo).save(acc);
    }

    @Test
    void deleteAccount_delegatesToDeleteById() {
        service.deleteAccount(123L);
        verify(repo).deleteById(123L);
    }

    @Test
    void findAll_delegatesToFindAll() {
        List<Account> list = Arrays.asList(new Account(), new Account());
        when(repo.findAll()).thenReturn(list);
        assertThat(service.findAll()).isEqualTo(list);
    }

    @Test
    void getById_delegatesToGetReferenceById() {
        Account acc = new Account();
        when(repo.getReferenceById(7L)).thenReturn(acc);
        assertThat(service.getById(7L)).isSameAs(acc);
    }
}