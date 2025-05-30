package resenkov.work.t1business.aop;

import jakarta.transaction.Transactional;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import resenkov.work.t1business.entity.TimeLimitLog;
import resenkov.work.t1business.repository.TimeLimitLogRepository;


import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Aspect
@Component
public class MetricAspect {

    private final String TOPIC_NAME = "t1_demo_metrics";
    private final TimeLimitLogRepository timeLimitLogRepository;
    private final long timeLimit;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    public MetricAspect(@Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> stringKafkaTemplate, TimeLimitLogRepository timeLimitLogRepository, @Value("${app.metric.timeLimit}") long timeLimit, KafkaTemplate<String, String> kafkaTemplate, KafkaTemplate<String, String> stringKafkaTemplate1) {
        this.timeLimitLogRepository = timeLimitLogRepository;
        this.timeLimit = timeLimit;
        this.stringKafkaTemplate = stringKafkaTemplate1;
    }

    @Around("@annotation(resenkov.work.t1business.aop.Metric)")
    @Transactional
    public Object check(ProceedingJoinPoint pjp) throws Throwable{
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long end = System.currentTimeMillis();
        long time = end - start;
        if (time > timeLimit) {
            MethodSignature sig = (MethodSignature) pjp.getSignature();
            TimeLimitLog log = new TimeLimitLog();
            System.out.println("Вызван метод:  " + sig.getName() + " и его время выполнения " + time);
            log.setMethodName(sig.getName());
            log.setDuration(time);
            log.setTimestamp(LocalDateTime.now());
            try {
                sendMetricMessage(pjp);
            } catch (Exception e) {
                timeLimitLogRepository.save(log);
            }
        }
        return result;
    }

    private void sendMetricMessage(ProceedingJoinPoint pjp) {
            String message = String.format("Метод %s превысил лимит времени!", pjp.getSignature());
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, message);
            record.headers().add("errorType", "METRICS".getBytes(StandardCharsets.UTF_8));
            stringKafkaTemplate.send(record);
    }
}
