package resenkov.work.t1business.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class UnlockMetrics {
    private final Counter clientUnlockCounter;
    private final Counter accountUnlockCounter;

    public UnlockMetrics(MeterRegistry meterRegistry) {
        clientUnlockCounter = Counter.builder("transaction.unlock_operations.clients")
                .description("Total client unlock operations initiated")
                .register(meterRegistry);

        accountUnlockCounter = Counter.builder("transaction.unlock_operations.accounts")
                .description("Total account unlock operations initiated")
                .register(meterRegistry);
    }

    public void incrementClientUnlock() {
        clientUnlockCounter.increment();
    }

    public void incrementAccountUnlock() {
        accountUnlockCounter.increment();
    }
}