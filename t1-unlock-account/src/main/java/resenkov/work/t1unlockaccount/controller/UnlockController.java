package resenkov.work.t1unlockaccount.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import resenkov.work.t1unlockaccount.UnlockService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/unlock")
public class UnlockController {
    private final UnlockService unlockService;

    public UnlockController(UnlockService unlockService) {
        this.unlockService = unlockService;
    }

    @PostMapping("/clients")
    public ResponseEntity<UnlockResponse> unlockClients(@RequestBody UnlockRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return ResponseEntity.badRequest().body(new UnlockResponse("Client IDs required"));
        }

        List<Long> unlockedIds = unlockService.unlockClients(request.getIds());
        return ResponseEntity.ok(new UnlockResponse(unlockedIds));
    }

    @PostMapping("/accounts")
    public ResponseEntity<UnlockResponse> unlockAccounts(@RequestBody UnlockRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return ResponseEntity.badRequest().body(new UnlockResponse("Account IDs required"));
        }

        List<Long> unlockedIds = unlockService.unlockAccounts(request.getIds());
        return ResponseEntity.ok(new UnlockResponse(unlockedIds));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnlockRequest {
        private List<Long> ids;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnlockResponse {
        private String message;
        private List<Long> unlockedIds;
        private Instant timestamp = Instant.now();

        public UnlockResponse(List<Long> unlockedIds) {
            this.unlockedIds = unlockedIds;
            this.message = "Unlocked " + unlockedIds.size() + " items";
        }

        public UnlockResponse(String error) {
            this.message = error;
            this.unlockedIds = Collections.emptyList();
        }
    }
}