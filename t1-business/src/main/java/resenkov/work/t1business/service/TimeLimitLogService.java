package resenkov.work.t1business.service;

import org.springframework.stereotype.Service;
import resenkov.work.t1metricsstarter.entity.TimeLimitLog;
import resenkov.work.t1metricsstarter.repository.TimeLimitLogRepository;

import java.util.List;

@Service
public class TimeLimitLogService {
    private TimeLimitLogRepository timeLimitLogRepository;
    public List<TimeLimitLog> findAll(){
        return timeLimitLogRepository.findAll();
    }
}
