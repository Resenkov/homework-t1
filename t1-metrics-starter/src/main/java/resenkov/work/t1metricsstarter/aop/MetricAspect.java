package resenkov.work.t1metricsstarter.aop;

import jakarta.transaction.Transactional;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import resenkov.work.t1metricsstarter.config.MetricsProperties;
import resenkov.work.t1metricsstarter.entity.TimeLimitLog;
import resenkov.work.t1metricsstarter.repository.TimeLimitLogRepository;


import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Aspect
@Component
public class MetricAspect {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TimeLimitLogRepository repo;
    private final MetricsProperties props;

    public MetricAspect(KafkaTemplate<String, String> kafkaTemplate, TimeLimitLogRepository repo, MetricsProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.repo = repo;
        this.props = props;
    }

    @Around("@annotation(resenkov.work.t1metricsstarter.aop.Metric)")
    @Transactional
    public Object check(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object res = pjp.proceed();
        long duration = System.currentTimeMillis() - start;
        if (duration > props.getTimeLimit()) {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            TimeLimitLog log = new TimeLimitLog();
            log.setMethodName(sig.getName());
            log.setDuration(duration);
            log.setTimestamp(LocalDateTime.now());
            try {
                String msg = String.format("Метод %s превысил лимит %dms",
                        sig.getName(), props.getTimeLimit());
                ProducerRecord<String,String> record =
                        new ProducerRecord<>(props.getTopic(), msg);
                record.headers().add("errorType",
                        "METRICS".getBytes(StandardCharsets.UTF_8));
                kafkaTemplate.send(record).get();  // чтобы получить исключение при падении
            } catch (Exception e) {
                repo.save(log);
            }
        }
        return res;
    }
}