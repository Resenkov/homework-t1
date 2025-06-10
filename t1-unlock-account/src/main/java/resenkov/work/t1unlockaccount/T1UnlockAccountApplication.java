package resenkov.work.t1unlockaccount;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableJpaRepositories(basePackages = {
		"resenkov.work.t1entity.repository",
})
@EntityScan(basePackages = {
		"resenkov.work.t1entity.entity",
})
@ComponentScan(basePackages = {
		"resenkov.work.t1unlockaccount",
		"resenkov.work.t1entity",
})
@EnableScheduling
public class T1UnlockAccountApplication {
	public static void main(String[] args) {
		SpringApplication.run(T1UnlockAccountApplication.class, args);
	}
}
