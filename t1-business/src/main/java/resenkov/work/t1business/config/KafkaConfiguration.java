package resenkov.work.t1business.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import resenkov.work.t1business.entity.Transaction;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 10000);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 30000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60000);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000);
        return config;
    }

    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> config = baseConfig();
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ProducerFactory<String, Transaction> transactionProducerFactory() {
        Map<String, Object> config = baseConfig();
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean(name = "stringKafkaTemplate")
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    @Bean(name = "transactionKafkaTemplate")
    public KafkaTemplate<String, Transaction> transactionKafkaTemplate() {
        return new KafkaTemplate<>(transactionProducerFactory());
    }
}