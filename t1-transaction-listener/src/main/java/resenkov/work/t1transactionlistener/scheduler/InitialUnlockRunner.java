package resenkov.work.t1transactionlistener.scheduler;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialUnlockRunner implements ApplicationRunner {

    private final UnlockScheduler unlockScheduler;

    public InitialUnlockRunner(UnlockScheduler unlockScheduler) {
        this.unlockScheduler = unlockScheduler;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        unlockScheduler.unlockClientsTask();
        unlockScheduler.unlockAccountsTask();

        System.out.println("Initial unlock requests sent successfully!");
    }
}