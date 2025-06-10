package resenkov.work.t1unlockaccount.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import resenkov.work.t1unlockaccount.service.UnlockService;

@RestController
@RequestMapping("/unlock")
public class UnlockController {
    private final UnlockService unlockService;

    @Value("${unlock.batch-size.clients:3}")
    private int clientBatchSize;
    @Value("${unlock.batch-size.accounts:3}")
    private int accountBatchSize;

    public UnlockController(UnlockService unlockService) {
        this.unlockService = unlockService;
    }

    @PostMapping("/clients")
    public ResponseEntity<String> unlockClients() {
        int count = unlockService.unlockClients(clientBatchSize);
        return ResponseEntity
                .ok("Unlocked " + count + " clients");
    }

    @PostMapping("/accounts")
    public ResponseEntity<String> unlockAccounts() {
        int count = unlockService.unlockAccounts(accountBatchSize);
        return ResponseEntity
                .ok("Unlocked " + count + " accounts");
    }
}