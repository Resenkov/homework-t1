package resenkov.work.t1transactionlistener.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1transactionlistener.client.UnlockServiceClient;
import resenkov.work.t1transactionlistener.config.UnlockProperties;
import resenkov.work.t1transactionlistener.service.BlockedEntitiesService;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UnlockScheduler {
    private static final Logger logger = LoggerFactory.getLogger(UnlockScheduler.class);

    private final BlockedEntitiesService blockedEntitiesService;
    private final UnlockServiceClient unlockServiceClient;
    private final UnlockProperties properties;

    public UnlockScheduler(BlockedEntitiesService blockedEntitiesService,
                           UnlockServiceClient unlockServiceClient,
                           UnlockProperties properties) {
        this.blockedEntitiesService = blockedEntitiesService;
        this.unlockServiceClient = unlockServiceClient;
        this.properties = properties;
    }

    public void unlockClientsTask() {
        try {
            logger.info("Starting clients unlock task...");
            List<Client> blockedClients = blockedEntitiesService.findBlockedClients(
                    properties.getClientLimit()
            );

            if (!blockedClients.isEmpty()) {
                List<Long> clientIds = blockedClients.stream()
                        .map(Client::getId)
                        .collect(Collectors.toList());

                unlockServiceClient.requestClientUnlock(clientIds);
                logger.info("Sent unlock request for {} clients", clientIds.size());
            } else {
                logger.info("No blocked clients found to unlock");
            }
        } catch (Exception e) {
            logger.error("Error during clients unlock task", e);
        }
    }

    public void unlockAccountsTask() {
        try {
            logger.info("Starting accounts unlock task...");
            List<Account> arrestedAccounts = blockedEntitiesService.findArrestedAccounts(
                    properties.getAccountLimit()
            );

            if (!arrestedAccounts.isEmpty()) {
                List<Long> accountIds = arrestedAccounts.stream()
                        .map(Account::getId)
                        .collect(Collectors.toList());

                unlockServiceClient.requestAccountUnlock(accountIds);
                logger.info("Sent unlock request for {} accounts", accountIds.size());
            } else {
                logger.info("No arrested accounts found to unlock");
            }
        } catch (Exception e) {
            logger.error("Error during accounts unlock task", e);
        }
    }

    @Scheduled(fixedRateString = "${unlock.period}")
    public void scheduledUnlockClients() {
        unlockClientsTask();
    }

    @Scheduled(fixedRateString = "${unlock.period}")
    public void scheduledUnlockAccounts() {
        unlockAccountsTask();
    }
}