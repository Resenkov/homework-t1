package resenkov.work.t1business.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import resenkov.work.t1entity.entity.Account;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.repository.AccountRepository;
import resenkov.work.t1entity.repository.ClientRepository;

import java.util.List;

@Service
public class BlockedEntitiesService {
    private final ClientRepository clientRepository;
    private final AccountRepository accountRepository;

    public BlockedEntitiesService(ClientRepository clientRepository,
                                  AccountRepository accountRepository) {
        this.clientRepository = clientRepository;
        this.accountRepository = accountRepository;
    }

    public List<Client> findBlockedClients(int limit) {
        return clientRepository.findByStatus(
                Client.Status.BLOCKED,
                PageRequest.of(0, limit)
        );
    }

    public List<Account> findArrestedAccounts(int limit) {
        return accountRepository.findByStatus(
                Account.Status.ARRESTED,
                PageRequest.of(0, limit)
        );
    }
}