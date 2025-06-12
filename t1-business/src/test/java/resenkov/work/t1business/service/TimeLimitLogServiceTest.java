package resenkov.work.t1business.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resenkov.work.t1metricsstarter.entity.TimeLimitLog;
import resenkov.work.t1metricsstarter.repository.TimeLimitLogRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeLimitLogServiceTest {

    @Mock
    TimeLimitLogRepository repo;

    @InjectMocks
    TimeLimitLogService service;

    @Test
    void findAll_delegatesToFindAll() {
        List<TimeLimitLog> logs = Arrays.asList(new TimeLimitLog(), new TimeLimitLog());
        when(repo.findAll()).thenReturn(logs);

        assertThat(service.findAll()).isEqualTo(logs);
    }
}
