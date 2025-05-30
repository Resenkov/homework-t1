package resenkov.work.t1business.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import resenkov.work.t1business.entity.DataSourceErrorLog;
import resenkov.work.t1business.repository.DataSourceErrorLogRepository;


import java.util.List;


@Service
@Transactional
public class DataSourceErrorLogService {
    private final DataSourceErrorLogRepository dataSourceErrorLogRepository;

    public DataSourceErrorLogService(DataSourceErrorLogRepository dataSourceErrorLogRepository) {
        this.dataSourceErrorLogRepository = dataSourceErrorLogRepository;
    }

    public List<DataSourceErrorLog> findAll(){
        return dataSourceErrorLogRepository.findAll();
    }
}
