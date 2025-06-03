package resenkov.work.t1checktransaction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import resenkov.work.t1checktransaction.repository.BlacklistRepository;

@Service
public class BlacklistService {
    private final BlacklistRepository blacklistRepository;

    @Autowired
    public BlacklistService(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    public boolean isClientInBlacklist(Long clientId) {
        return blacklistRepository.existsByClientId(clientId);
    }
}