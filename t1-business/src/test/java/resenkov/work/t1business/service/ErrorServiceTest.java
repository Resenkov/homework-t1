package resenkov.work.t1business.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resenkov.work.t1entity.repository.AccountRepository;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorServiceTest {

    @Mock
    AccountRepository accountRepo;

    @InjectMocks
    ErrorService service;

    @Test
    void triggerError_callsSaveWithNull() {
        // ничего не выбрасываем, т.к. Mockito по умолчанию просто вернёт null
        service.triggerError();
        verify(accountRepo).save(null);
    }

    @Test
    void slow_sleepsWithoutException() throws InterruptedException {
        // просто проверяем, что метод не выбрасывает
        service.slow();
    }
}
