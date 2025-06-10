package resenkov.work.t1transactionlistener.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "unlock")
public class UnlockProperties {
    private Duration period;
    private int clientLimit;
    private int accountLimit;
}