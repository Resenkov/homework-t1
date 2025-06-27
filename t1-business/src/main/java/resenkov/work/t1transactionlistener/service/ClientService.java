package resenkov.work.t1transactionlistener.service;

import org.springframework.stereotype.Service;
import resenkov.work.t1entity.entity.Client;
import resenkov.work.t1entity.repository.ClientRepository;
import resenkov.work.t1metricsstarter.aop.Cached;


import java.util.Optional;

@Service
public class ClientService {
    private final ClientRepository repo;
    public ClientService(ClientRepository repo) { this.repo = repo; }

    @Cached
    public Optional<Client> getClient(Long id) {
        System.out.println(">> ClientService: cache-miss, обращаюсь в БД для id=" + id);
        return repo.findById(id);
    }
}
