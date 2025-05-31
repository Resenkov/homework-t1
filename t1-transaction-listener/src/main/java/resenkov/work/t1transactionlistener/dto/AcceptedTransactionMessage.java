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
public class AcceptedTransactionMessage {

    private Long clientId;
    private Long accountId;
    private Long transactionId;
    private LocalDateTime timestamp;
    private BigDecimal amount;
    private BigDecimal balance;

}