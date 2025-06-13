package resenkov.work.t1transactionlistener.service;

import org.springframework.stereotype.Service;

import resenkov.work.t1entity.repository.AccountRepository;
import resenkov.work.t1metricsstarter.aop.LogDataError;
import resenkov.work.t1metricsstarter.aop.Metric;


@Service
public class ErrorService {
    private final AccountRepository accountRepo;
    public ErrorService(AccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    @LogDataError
    public void triggerError() {
        accountRepo.save(null);
    }

    @Metric
    public void slow() throws InterruptedException {
        Thread.sleep(50);
    }
}
