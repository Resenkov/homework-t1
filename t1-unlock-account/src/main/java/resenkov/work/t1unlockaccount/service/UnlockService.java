package resenkov.work.t1unlockaccount.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.repository.AccountRepository;
import resenkov.work.t1entity.repository.ClientRepository;
import resenkov.work.t1unlockaccount.config.UnlockDecisionMaker;

import java.util.ArrayList;
import java.util.List;

@Service
public class UnlockService {
    private static final Logger logger = LoggerFactory.getLogger(UnlockService.class);

    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;
    private final UnlockDecisionMaker decisionMaker;

    public UnlockService(ClientRepository clientRepository,
                         AccountRepository accountRepository,
                         UnlockDecisionMaker decisionMaker) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
        this.decisionMaker = decisionMaker;
    }

    public List<Long> unlockClients(List<Long> clientIds) {
        List<Client> clientsToUpdate = new ArrayList<>();
        List<Long> unlockedIds = new ArrayList<>();

        for (Long id : clientIds) {
            clientRepository.findById(id).ifPresent(client -> {
                if (client.getStatus() == Client.Status.BLOCKED && decisionMaker.shouldUnlock()) {
                    client.setStatus(Client.Status.ACTIVE);
                    clientsToUpdate.add(client);
                    unlockedIds.add(id);
                }
            });
        }

        if (!clientsToUpdate.isEmpty()) {
            clientRepository.saveAll(clientsToUpdate);
            logger.info("Unlocked {} clients", unlockedIds.size());
        }
        return unlockedIds;
    }

    public List<Long> unlockAccounts(List<Long> accountIds) {
        List<Account> accountsToUpdate = new ArrayList<>();
        List<Long> unlockedIds = new ArrayList<>();

        for (Long id : accountIds) {
            accountRepository.findById(id).ifPresent(account -> {
                if (account.getStatus() == Account.Status.ARRESTED && decisionMaker.shouldUnlock()) {
                    account.setStatus(Account.Status.OPEN);
                    accountsToUpdate.add(account);
                    unlockedIds.add(id);
                }
            });
        }

        if (!accountsToUpdate.isEmpty()) {
            accountRepository.saveAll(accountsToUpdate);
            logger.info("Unlocked {} accounts", unlockedIds.size());
        }
        return unlockedIds;
    }
}