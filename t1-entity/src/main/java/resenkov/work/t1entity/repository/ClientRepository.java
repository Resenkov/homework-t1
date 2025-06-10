package resenkov.work.t1entity.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import resenkov.work.t1entity.entity.Client;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByClientId(Long clientId);
    List<Client> findByStatus(Client.Status status, Pageable pageable);
}