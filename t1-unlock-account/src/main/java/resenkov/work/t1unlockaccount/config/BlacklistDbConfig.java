package resenkov.work.t1unlockaccount.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "resenkov.work.t1unlockaccount.repository",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = resenkov.work.t1unlockaccount.repository.UnlockClientRepository.class
        ),
        entityManagerFactoryRef = "blacklistEntityManagerFactory",
        transactionManagerRef = "blacklistTransactionManager"
)
public class BlacklistDbConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.blacklist")
    public DataSourceProperties blacklistDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource blacklistDataSource() {
        return blacklistDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "blacklistEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean blacklistEntityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(blacklistDataSource())
                .packages("resenkov.work.t1checktransaction.entity")
                .persistenceUnit("blacklist")
                .build();
    }

    @Bean(name = "blacklistTransactionManager")
    public PlatformTransactionManager blacklistTransactionManager(
            @Qualifier("blacklistEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
