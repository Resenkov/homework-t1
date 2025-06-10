package resenkov.work.t1entity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @JoinColumn(name = "account_id")
    private Long accountId;

    @NotNull
    public enum Status{
        ARRESTED,
        BLOCKED,
        CLOSED,
        OPEN
    }

    @NotNull
    public enum BalanceType {
        DEBIT,
        CREDIT
    }

    @Enumerated(EnumType.STRING)
    private BalanceType balanceType;

    private BigDecimal balance;

    private BigDecimal frozenAmount;
}