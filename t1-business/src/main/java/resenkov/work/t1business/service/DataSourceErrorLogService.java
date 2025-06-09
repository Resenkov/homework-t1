package resenkov.work.t1business.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import resenkov.work.t1metricsstarter.entity.DataSourceErrorLog;
import resenkov.work.t1metricsstarter.repository.DataSourceErrorLogRepository;


import java.util.List;


@Service
@Transactional
public class DataSourceErrorLogService {
    private DataSourceErrorLogRepository dataSourceErrorLogRepository;

    public List<DataSourceErrorLog> findAll(){
        return dataSourceErrorLogRepository.findAll();
    }
}
