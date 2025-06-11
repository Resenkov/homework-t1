package resenkov.work.t1transactionlistener.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.repository.AccountRepository;
import resenkov.work.t1entity.repository.ClientRepository;

import jakarta.annotation.PostConstruct;

@Service
public class BlockedMetricsService {
    private final MeterRegistry meterRegistry;
    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;

    private volatile long blockedClientsCount = 0;
    private volatile long arrestedAccountsCount = 0;

    public BlockedMetricsService(MeterRegistry meterRegistry,
                                 ClientRepository clientRepository,
                                 AccountRepository accountRepository) {
        this.meterRegistry = meterRegistry;
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
    }

    @PostConstruct
    public void init() {
        // Регистрация метрик
        Gauge.builder("transaction.blocked_clients.count",
                        () -> blockedClientsCount)
                .description("Number of blocked clients")
                .register(meterRegistry);

        Gauge.builder("transaction.arrested_accounts.count",
                        () -> arrestedAccountsCount)
                .description("Number of arrested accounts")
                .register(meterRegistry);

        // Первоначальное обновление
        updateMetrics();
    }

    @Scheduled(fixedRate = 60000) // Обновление каждую минуту
    public void updateMetrics() {
        blockedClientsCount = clientRepository.countByStatus(Client.Status.BLOCKED);
        arrestedAccountsCount = accountRepository.countByStatus(Account.Status.ARRESTED);
    }
}