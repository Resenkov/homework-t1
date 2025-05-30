package resenkov.work.t1business.service;

import org.springframework.stereotype.Service;
import resenkov.work.t1business.aop.LogDataError;
import resenkov.work.t1business.aop.Metric;
import resenkov.work.t1business.repository.AccountRepository;


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
