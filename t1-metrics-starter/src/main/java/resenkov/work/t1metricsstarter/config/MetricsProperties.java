package resenkov.work.t1metricsstarter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("t1.metrics")
public class MetricsProperties {
    private long timeLimit = 500;            // ms
    private String topic = "t1_demo_metrics";
}
