package resenkov.work.t1entity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @jakarta.persistence.Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    private String lastName;

    private Long clientId;

    public enum Status{
        BLOCKED,
        ACTIVE
    }

    @Enumerated(EnumType.STRING)
    private Status status;

    @NotNull
    private String firstName;
    @NotNull
    private String middleName;
}