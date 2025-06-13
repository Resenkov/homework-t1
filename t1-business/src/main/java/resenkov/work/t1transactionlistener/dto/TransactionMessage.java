package resenkov.work.t1transactionlistener.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMessage {
    private Long transactionId;
    private Long accountId;
    private Long clientId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
}
