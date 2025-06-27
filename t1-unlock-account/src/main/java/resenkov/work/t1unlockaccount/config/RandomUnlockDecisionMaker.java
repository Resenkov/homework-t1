package resenkov.work.t1unlockaccount.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomUnlockDecisionMaker implements UnlockDecisionMaker {
    @Value("${unlock.probability:0.5}")
    private double unlockProbability;

    @Override
    public boolean shouldUnlock() {
        return ThreadLocalRandom.current().nextDouble() < unlockProbability;
    }
}