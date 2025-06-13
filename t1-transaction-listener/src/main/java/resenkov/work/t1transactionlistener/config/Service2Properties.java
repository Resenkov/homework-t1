package resenkov.work.t1transactionlistener.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "service2")
@Getter
@Setter
public class Service2Properties {
    private String url;
    private String username;
    private String password;
}