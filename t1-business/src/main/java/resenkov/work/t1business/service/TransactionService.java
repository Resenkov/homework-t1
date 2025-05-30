package resenkov.work.t1business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import resenkov.work.t1business.entity.Transaction;
import resenkov.work.t1business.repository.TransactionRepository;


import java.util.List;

@RequiredArgsConstructor
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    public Transaction addTransaction(Transaction transaction){
        return transactionRepository.save(transaction);
    }

    public List<Transaction> findAll(){
        return transactionRepository.findAll();
    }

    public Transaction updateTransaction(Transaction transaction){
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id){
        transactionRepository.deleteById(id);
    }

    public Transaction getById(Long id){
        return transactionRepository.getReferenceById(id);
    }
}