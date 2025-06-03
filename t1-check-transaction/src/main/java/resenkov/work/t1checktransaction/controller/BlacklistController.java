package resenkov.work.t1checktransaction.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import resenkov.work.t1checktransaction.service.BlacklistService;

import java.util.Map;

@RestController
@Slf4j
public class BlacklistController {

    private final BlacklistService blacklistService;

    @Autowired
    public BlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @GetMapping("/check-blacklist")
    public ResponseEntity<?> checkBlacklist(
            @RequestParam("clientId") Long clientId,
            @RequestParam("accountId") Long accountId) {

        try {
            boolean isBlacklisted = blacklistService.isClientInBlacklist(clientId);

            String status = isBlacklisted ? "BLOCKED" : "OK";
            Map<String, String> body = Map.of(
                    "clientId", clientId.toString(),
                    "accountId", accountId.toString(),
                    "status", status
            );

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Error in checkBlacklist", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal server error",
                            "message", e.getMessage()
                    ));
        }
    }
}
