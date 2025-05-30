package resenkov.work.t1business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import resenkov.work.t1business.entity.Account;
import resenkov.work.t1business.repository.AccountRepository;


import java.util.List;

@RequiredArgsConstructor
@Service
public class AccountService {
    private final AccountRepository accountRepository;

    public Account addAccount(Account account){
        return accountRepository.save(account);
    }

    public Account updateAccount(Account account){
        return accountRepository.save(account);
    }

    public void deleteAccount(Long id){
        accountRepository.deleteById(id);
    }

    public List<Account> findAll(){
        return accountRepository.findAll();
    }
    public Account getById(Long id){
        return accountRepository.getReferenceById(id);
    }
}