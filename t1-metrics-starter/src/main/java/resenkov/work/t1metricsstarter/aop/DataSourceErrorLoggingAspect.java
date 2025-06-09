package resenkov.work.t1metricsstarter.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import resenkov.work.t1metricsstarter.entity.DataSourceErrorLog;
import resenkov.work.t1metricsstarter.repository.DataSourceErrorLogRepository;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.ProducerRecord;

@Aspect
@Component
public class DataSourceErrorLoggingAspect {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final DataSourceErrorLogRepository repo;

    public DataSourceErrorLoggingAspect(KafkaTemplate<String,String> kafkaTemplate,
                                        DataSourceErrorLogRepository repo) {
        this.kafkaTemplate = kafkaTemplate;
        this.repo = repo;
    }

    @Around("@annotation(resenkov.work.t1metricsstarter.aop.LogDataError)")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object logErrors(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } catch (Exception ex) {
            try {
                String msg = String.format("Метод %s вызвал ошибку", pjp.getSignature());
                ProducerRecord<String,String> record =
                        new ProducerRecord<>("t1_demo_metrics", msg);
                record.headers().add("errorType",
                        "DATA_SOURCE".getBytes(StandardCharsets.UTF_8));
                kafkaTemplate.send(record).get();
            } catch (Exception kafkaEx) {
                MethodSignature sig = (MethodSignature) pjp.getSignature();
                DataSourceErrorLog log = new DataSourceErrorLog();
                log.setSignatureMethod(sig.getDeclaringTypeName() + "." + sig.getName());
                log.setMessage(ex.getMessage());
                log.setStackTrace(getStackTrace(ex));
                repo.save(log);
            }
            throw ex;
        }
    }

    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
