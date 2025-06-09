package resenkov.work.t1metricsstarter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import resenkov.work.t1metricsstarter.aop.DataSourceErrorLoggingAspect;
import resenkov.work.t1metricsstarter.aop.MetricAspect;
import resenkov.work.t1metricsstarter.repository.DataSourceErrorLogRepository;
import resenkov.work.t1metricsstarter.repository.TimeLimitLogRepository;

@Configuration
@EnableConfigurationProperties(MetricsProperties.class)
@ConditionalOnClass({KafkaTemplate.class, org.aspectj.lang.ProceedingJoinPoint.class})
public class MetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MetricAspect metricAspect(TimeLimitLogRepository repo,
                                     MetricsProperties props,
                                     KafkaTemplate<String,String> kt) {
        return new MetricAspect(kt, repo, props);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceErrorLoggingAspect dataSourceErrorLoggingAspect(
            KafkaTemplate<String,String> kt,
            DataSourceErrorLogRepository repo) {
        return new DataSourceErrorLoggingAspect(kt, repo);
    }
}