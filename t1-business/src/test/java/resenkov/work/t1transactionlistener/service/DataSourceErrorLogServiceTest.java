package resenkov.work.t1transactionlistener.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import resenkov.work.t1metricsstarter.entity.DataSourceErrorLog;
import resenkov.work.t1metricsstarter.repository.DataSourceErrorLogRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSourceErrorLogServiceTest {

    @Mock
    DataSourceErrorLogRepository repo;

    @InjectMocks
    DataSourceErrorLogService service;

    @Test
    void findAll_delegatesToFindAll() {
        List<DataSourceErrorLog> logs = Arrays.asList(
                new DataSourceErrorLog(), new DataSourceErrorLog()
        );
        when(repo.findAll()).thenReturn(logs);

        List<DataSourceErrorLog> result = service.findAll();
        assertThat(result).isEqualTo(logs);
    }
}
