package resenkov.work.t1transactionlistener.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import resenkov.work.t1transactionlistener.service.TransactionService;
import resenkov.work.t1entity.entity.Transaction;


import java.util.List;

@RestController
@RequestMapping("/transactions")
@Log4j2
public class TransactionController {

    private final String TOPIC_NAME = "t1_demo_transactions";
    private final TransactionService transactionService;
    private final KafkaTemplate<String, Transaction> transactionKafkaTemplate;

    public TransactionController(@Qualifier("transactionKafkaTemplate") KafkaTemplate<String, Transaction> transactionKafkaTemplate,TransactionService transactionService) {
        this.transactionService = transactionService;
        this.transactionKafkaTemplate = transactionKafkaTemplate;
    }

    @GetMapping("/all")
    public ResponseEntity<List<Transaction>> getAll(){
        return ResponseEntity.ok(transactionService.findAll());
    }

    @PostMapping("/get/{id}")
    public ResponseEntity<Transaction> getById(Long id){
        return ResponseEntity.ok(transactionService.getById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<Transaction> addAccount(Transaction  transaction){
        Transaction savedTransaction = transactionService.addTransaction(transaction);
        transactionKafkaTemplate.send(TOPIC_NAME,transaction).toCompletableFuture()
                .whenComplete((result, ex) ->{
                    if(ex != null){
                        log.error("Ошибка при отправке сообщения в топик!", ex);
                    }else {
                        log.info("Сообщение успешно отправлено в топик!", savedTransaction);
                    }
                });
        return ResponseEntity.ok(savedTransaction);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity deleteById(Long id){
        transactionService.deleteTransaction(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/update")
    public ResponseEntity<Transaction> updateAccount(Transaction transaction){
        return ResponseEntity.ok(transactionService.updateTransaction(transaction));
    }
}
