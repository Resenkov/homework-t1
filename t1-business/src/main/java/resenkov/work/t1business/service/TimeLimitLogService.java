package resenkov.work.t1business.service;

import org.springframework.stereotype.Service;
import resenkov.work.t1business.entity.TimeLimitLog;
import resenkov.work.t1business.repository.TimeLimitLogRepository;

import java.util.List;

@Service
public class TimeLimitLogService {
    private final TimeLimitLogRepository timeLimitLogRepository;

    public TimeLimitLogService(TimeLimitLogRepository timeLimitLogRepository) {
        this.timeLimitLogRepository = timeLimitLogRepository;
    }

    public List<TimeLimitLog> findAll(){
        return timeLimitLogRepository.findAll();
    }
}
