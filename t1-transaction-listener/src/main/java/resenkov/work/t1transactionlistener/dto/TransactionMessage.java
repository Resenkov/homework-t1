package resenkov.work.t1transactionlistener.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import resenkov.work.t1business.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMessage {
    @JsonProperty("transactionId") Long transactionId;
    @JsonProperty("accountId")   Long accountId;
    @JsonProperty("clientId")    Long clientId;
    @JsonProperty("amount")      BigDecimal amount;
    @JsonProperty("status")      Transaction.Status status;
    @JsonProperty("timestamp")   LocalDateTime timestamp;
}