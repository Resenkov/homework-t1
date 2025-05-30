package resenkov.work.t1business.service;

import org.springframework.stereotype.Service;
import resenkov.work.t1business.aop.Cached;
import resenkov.work.t1business.entity.Client;
import resenkov.work.t1business.repository.ClientRepository;


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
