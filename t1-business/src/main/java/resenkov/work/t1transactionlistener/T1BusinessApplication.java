package resenkov.work.t1transactionlistener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {
		"resenkov.work.t1entity.repository",
		"resenkov.work.t1metricsstarter.repository"
})
@EntityScan(basePackages = {
		"resenkov.work.t1entity.entity",
		"resenkov.work.t1metricsstarter.entity"
})
@ComponentScan(basePackages = {
        "resenkov.work.t1transactionlistener",
		"resenkov.work.t1entity",
		"resenkov.work.t1metricsstarter"
})
public class T1BusinessApplication {

	public static void main(String[] args) {
		SpringApplication.run(T1BusinessApplication.class, args);
	}

}
