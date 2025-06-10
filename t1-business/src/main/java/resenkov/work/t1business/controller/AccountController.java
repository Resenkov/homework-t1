package resenkov.work.t1business.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resenkov.work.t1business.service.AccountService;
import resenkov.work.t1entity.entity.Account;


import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/all")
    public ResponseEntity<List<Account>> getAll(){
        return ResponseEntity.ok(accountService.findAll());
    }

    @PostMapping("get/{id}")
    public ResponseEntity<Account> getById(Long id){
        return ResponseEntity.ok(accountService.getById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<Account> addAccount(Account account){
        return ResponseEntity.ok(accountService.addAccount(account));
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity deleteById(Long id){
        accountService.deleteAccount(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/update")
    public ResponseEntity<Account> updateAccount(Account account){
        return ResponseEntity.ok(accountService.updateAccount(account));
    }
}
