package resenkov.work.t1transactionlistener.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import resenkov.work.t1transactionlistener.dto.TransactionMessage;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, TransactionMessage> consumerFactory(ObjectMapper objectMapper) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "transaction-processor");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // Убираем строку: props.put(JsonDeserializer.TRUSTED_PACKAGES, "resenkov.work.t1transactionlistener.dto");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Создаём JsonDeserializer, указывая целевой класс и отключая type headers
        JsonDeserializer<TransactionMessage> valueDeserializer =
                new JsonDeserializer<>(TransactionMessage.class, objectMapper, false);
        // Явно задаём, из каких пакетов разрешена десериализация
        valueDeserializer.addTrustedPackages("resenkov.work.t1transactionlistener.dto");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionMessage> kafkaListenerContainerFactory(
            ConsumerFactory<String, TransactionMessage> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, TransactionMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
