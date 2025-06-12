package resenkov.work.t1business.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.Base64;

@Configuration
public class WebClientConfig {

    @Value("${service2.url}")
    private String service2Url;

    @Value("${service2.username}")
    private String service2Username;

    @Value("${service2.password}")
    private String service2Password;

    @Bean
    public WebClient service2WebClient() throws SSLException {
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext))
                .responseTimeout(Duration.ofSeconds(5));

        return WebClient.builder()
                .baseUrl(service2Url)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " +
                        Base64.getEncoder().encodeToString(
                                (service2Username + ":" + service2Password).getBytes()
                        ))
                .build();
    }
}