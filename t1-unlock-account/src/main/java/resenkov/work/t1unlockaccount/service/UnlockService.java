package resenkov.work.t1unlockaccount.service;


import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import resenkov.work.t1business.entity.Account;
import resenkov.work.t1business.entity.Client;
import resenkov.work.t1business.repository.AccountRepository;
import resenkov.work.t1business.repository.ClientRepository;
import resenkov.work.t1checktransaction.entity.Blacklist;
import resenkov.work.t1unlockaccount.repository.UnlockAccountRepository;
import resenkov.work.t1unlockaccount.repository.UnlockClientRepository;

import java.util.List;

@Service
@Transactional
@Log4j2
public class UnlockService {
    private final ClientRepository clientRepository;
    private final UnlockClientRepository unlockClientRepository;
    private final UnlockAccountRepository unlockAccountRepository;
    private final AccountRepository accountRepository;

    public UnlockService(ClientRepository clientRepository, UnlockClientRepository unlockClientRepository, UnlockAccountRepository unlockAccountRepository, AccountRepository accountRepository) {
        this.clientRepository = clientRepository;
        this.unlockClientRepository = unlockClientRepository;
        this.unlockAccountRepository = unlockAccountRepository;
        this.accountRepository = accountRepository;
    }

    public int unlockClients(int limit) {
        List<Blacklist> batch = unlockClientRepository.findRandomBlacklistEntries(limit);
        batch.forEach(this::unlockOneClient);
        return batch.size();
    }

    public int unlockAccounts(int limit) {
        List<Account> batch = unlockAccountRepository.findBlockedAccountsRandomly(limit);
        batch.forEach(acc -> acc.setStatus(Account.Status.OPEN));
        accountRepository.saveAll(batch);
        log.info("Unlocked {} accounts", batch.size());
        return batch.size();
    }

    private void unlockOneClient(Blacklist b) {
        clientRepository.findByClientId(b.getClientId())
                .ifPresentOrElse(client -> {
                    client.setStatus(Client.Status.ACTIVE);
                    log.info("Unlocking client with id {}", b.getClientId());
                    clientRepository.save(client);
                    unlockClientRepository.delete(b);
                }, () -> {
                    log.warn("Client {} not found in main DB", b.getClientId());
                });
    }
}