package resenkov.work.t1business.generate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


import resenkov.work.t1business.entity.Account;
import resenkov.work.t1business.entity.Client;
import resenkov.work.t1business.entity.Transaction;
import resenkov.work.t1business.repository.AccountRepository;
import resenkov.work.t1business.repository.ClientRepository;
import resenkov.work.t1business.repository.TransactionRepository;
import resenkov.work.t1business.service.ClientService;
import resenkov.work.t1business.service.ErrorService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Component
public class TestGenerate implements CommandLineRunner {
    private final ClientRepository clientRepo;
    private final AccountRepository accountRepo;
    private final TransactionRepository txRepo;
    private final ErrorService errorService;
    private final ClientService clientService;

    public TestGenerate(ClientRepository clientRepo,
                        AccountRepository accountRepo,
                        TransactionRepository txRepo, ErrorService errorService, ClientService clientService) {
        this.clientRepo = clientRepo;
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.errorService = errorService;
        this.clientService = clientService;
    }

    @Override
    public void run(String... args) throws Exception {
        txRepo.deleteAll();
        accountRepo.deleteAll();
        clientRepo.deleteAll();

        Random rnd = new Random();
        for (int i = 1; i <= 4; i++) {
            Client client = new Client();
            client.setFirstName("Имя " + i);
            client.setLastName("Фамилия " + i);
            client.setMiddleName("Отчество " + i);
            client = clientRepo.save(client);

            Account account = new Account();
            account.setClient(client);
            account.setStatus(rnd.nextBoolean() ? Account.Status.OPEN : Account.Status.BLOCKED);
            account.setBalanceType(rnd.nextBoolean() ? Account.BalanceType.DEBIT : Account.BalanceType.CREDIT);
            account.setBalance(BigDecimal.valueOf(rnd.nextDouble() * 10000));
            account = accountRepo.save(account);

            for (int j = 0; j < 5; j++) {
                Transaction tx = new Transaction();
                tx.setAccount(account);
                tx.setTranscationId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
                tx.setCreatedAt(LocalDateTime.now());
                tx.setStatus(rnd.nextBoolean() ? Transaction.Status.ACCEPTED : Transaction.Status.REQUESTED);
                tx.setSum(BigDecimal.valueOf((rnd.nextBoolean() ? 1 : -1) * rnd.nextDouble() * 1000));
                txRepo.save(tx);
            }
        }

        System.out.println(">> 0) Сеем клиента в базу id=1");
        Client seed = new Client();
        seed.setFirstName("Тест");
        seed.setLastName("Клиент");
        seed.setMiddleName("Demo");
        seed = clientRepo.save(seed);
        System.out.println("\n>> 1) Демонстрация @Cached");
        clientService.getClient(seed.getId());
        clientService.getClient(seed.getId());

        System.out.println("\n>> 2) Демонстрация @Metric");
        errorService.slow();

        System.out.println("\n>> 3) Демонстрация @LogDataError");
        try {
            errorService.triggerError();
        } catch (Exception ignored) {
            System.out.println(">> Искусственная ошибка обработана");
        }
    }
}

