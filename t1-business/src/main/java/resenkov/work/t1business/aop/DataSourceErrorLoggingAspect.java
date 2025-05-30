package resenkov.work.t1business.aop;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import resenkov.work.t1business.entity.DataSourceErrorLog;
import resenkov.work.t1business.repository.DataSourceErrorLogRepository;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

@Aspect
@Component
public class DataSourceErrorLoggingAspect {

    private final DataSourceErrorLogRepository logRepo;
    private final String TOPIC_NAME = "t1_demo_metrics";
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    public DataSourceErrorLoggingAspect(@Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> stringKafkaTemplate,DataSourceErrorLogRepository logRepo, KafkaTemplate<String, String> kafkaTemplate) {
        this.logRepo = logRepo;
        this.stringKafkaTemplate = stringKafkaTemplate;
    }

    @Around("@annotation(resenkov.work.t1business.aop.LogDataError)")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Object logErrors(ProceedingJoinPoint pjp) throws Throwable {

        try {
            return pjp.proceed();
        } catch (Exception ex) {
            try{
                sendMetricMessage(pjp);
            }catch (Exception e){
                MethodSignature sig = (MethodSignature) pjp.getSignature();
                System.out.println("Вызван метод:  " + sig.getName() + " и завершился с ошибкой " + ex);
                DataSourceErrorLog log = new DataSourceErrorLog();
                log.setMessage(ex.getMessage());
                log.setStackTrace(getStackTrace(ex));
                log.setSignatureMethod(sig.getDeclaringTypeName() + "." + sig.getName());
                logRepo.save(log);
            }
            throw ex;
        }
    }

    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private void sendMetricMessage(ProceedingJoinPoint pjp) {
        String message = String.format("Метод %s вызвал ошибку!", pjp.getSignature());
        ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, message);
        record.headers().add("errorType", "DATA_SOURCE".getBytes(StandardCharsets.UTF_8));
        stringKafkaTemplate.send(record);
    }
}
