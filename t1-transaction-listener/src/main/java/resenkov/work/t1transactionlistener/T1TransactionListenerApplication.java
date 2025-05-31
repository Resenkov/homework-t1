package resenkov.work.t1transactionlistener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(
        basePackages = { "resenkov.work.t1business.repository" }
)
@EntityScan(
        basePackages = { "resenkov.work.t1business.entity" }
)
public class T1TransactionListenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(T1TransactionListenerApplication.class, args);
    }

}
