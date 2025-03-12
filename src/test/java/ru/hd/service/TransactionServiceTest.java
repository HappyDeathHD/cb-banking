package ru.hd.service;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hd.exception.BankingOperationException;
import ru.hd.exception.InvalidAccountStatusException;
import ru.hd.exception.InvalidCurrencyException;
import ru.hd.jpa.Account;
import ru.hd.jpa.Client;
import ru.hd.jpa.TransactionRecord;
import ru.hd.model.AccountStatus;
import ru.hd.model.Currency;
import ru.hd.model.TransactionType;
import testutil.TestDataGenerator;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static EntityManagerFactory entityManagerFactory;
    private TransactionService transactionService;

    @BeforeAll
    static void setupEntityManagerFactory() {
        entityManagerFactory = Persistence.createEntityManagerFactory("test-persistence-unit");
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void setup() {
        transactionService = new TransactionService();
    }

    @AfterEach
    void rollbackTransaction() {
        try (Session session = getNewSession()) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        }
    }

    @Test
    void testAccountPersistence() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client);

            session.persist(account);
            session.flush();
            session.detach(account);

            Account persisted = session.find(Account.class, account.getId());
            assertAll(
                    "Проверка сохраненного счета",
                    () -> assertNotNull(persisted.getId()),
                    () -> assertEquals(account.getAccountNumber(), persisted.getAccountNumber()),
                    () -> assertEquals(client.getId(), persisted.getClient().getId())
            );
        }
    }

    @Test
    void testSuccessfulTransfer() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(1000));
            Account to = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(from);
            session.persist(to);
            session.getTransaction().commit();

            transactionService.transfer(session, from.getId(), to.getId(), BigDecimal.valueOf(200));

            session.beginTransaction();
            session.refresh(from);
            session.refresh(to);
            assertEquals(800, from.getBalance().intValue());
            assertEquals(700, to.getBalance().intValue());
        }
    }

    @Test
    void testRollbackOnInvalidCurrency() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(1000), Currency.RUB);
            Account to = createTestAccount(client, BigDecimal.valueOf(500), Currency.USD);
            session.persist(from);
            session.persist(to);
            session.getTransaction().commit();

            assertThrows(InvalidCurrencyException.class,
                    () -> transactionService.transfer(session, from.getId(), to.getId(), BigDecimal.valueOf(200)));

            session.beginTransaction();
            assertEquals(1000, session.find(Account.class, from.getId()).getBalance().intValue());
            assertEquals(500, session.find(Account.class, to.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferWithNegativeAmount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(1000));
            Account to = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(from);
            session.persist(to);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, from.getId(), to.getId(), BigDecimal.valueOf(-200)));

            session.beginTransaction();
            assertEquals(1000, session.find(Account.class, from.getId()).getBalance().intValue());
            assertEquals(500, session.find(Account.class, to.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferWithZeroAmount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(1000));
            Account to = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(from);
            session.persist(to);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, from.getId(), to.getId(), BigDecimal.ZERO));

            session.beginTransaction();
            assertEquals(1000, session.find(Account.class, from.getId()).getBalance().intValue());
            assertEquals(500, session.find(Account.class, to.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferFromClosedAccount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(1000));
            from.setStatus(AccountStatus.CLOSED);
            Account to = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(from);
            session.persist(to);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, from.getId(), to.getId(), BigDecimal.valueOf(200)));

            session.beginTransaction();
            assertEquals(1000, session.find(Account.class, from.getId()).getBalance().intValue());
            assertEquals(500, session.find(Account.class, to.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferToClosedAccount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(1000));
            Account to = createTestAccount(client, BigDecimal.valueOf(500));
            to.setStatus(AccountStatus.CLOSED);
            session.persist(from);
            session.persist(to);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, from.getId(), to.getId(), BigDecimal.valueOf(200)));

            session.beginTransaction();
            assertEquals(1000, session.find(Account.class, from.getId()).getBalance().intValue());
            assertEquals(500, session.find(Account.class, to.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferWithInsufficientFunds() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(100));
            Account to = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(from);
            session.persist(to);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, from.getId(), to.getId(), BigDecimal.valueOf(200)));

            session.beginTransaction();
            assertEquals(100, session.find(Account.class, from.getId()).getBalance().intValue());
            assertEquals(500, session.find(Account.class, to.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferToSameAccount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(1000));
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, account.getId(), account.getId(), BigDecimal.valueOf(200)));

            session.beginTransaction();
            assertEquals(1000, session.find(Account.class, account.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferWithNonExistentFromAccount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account to = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(to);
            session.getTransaction().commit();

            Long nonExistentAccountId = ThreadLocalRandom.current().nextLong(1_000_000L);

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, nonExistentAccountId, to.getId(), BigDecimal.valueOf(200)));

            session.beginTransaction();
            assertEquals(500, session.find(Account.class, to.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferWithNonExistentToAccount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(1000));
            session.persist(from);
            session.getTransaction().commit();

            Long nonExistentAccountId = ThreadLocalRandom.current().nextLong(1_000_000L);

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, from.getId(), nonExistentAccountId, BigDecimal.valueOf(200)));

            session.beginTransaction();
            assertEquals(1000, session.find(Account.class, from.getId()).getBalance().intValue());
        }
    }

    @Test
    void testTransferWithNullAmount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account from = createTestAccount(client, BigDecimal.valueOf(1000));
            Account to = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(from);
            session.persist(to);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.transfer(session, from.getId(), to.getId(), null));

            session.beginTransaction();
            assertEquals(1000, session.find(Account.class, from.getId()).getBalance().intValue());
            assertEquals(500, session.find(Account.class, to.getId()).getBalance().intValue());
        }
    }

    @Test
    void testSuccessfulDeposit() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(account);
            session.getTransaction().commit();

            transactionService.depositToAccount(session, account, BigDecimal.valueOf(300));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(800, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testDepositToClosedAccount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            account.setStatus(AccountStatus.CLOSED);
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(InvalidAccountStatusException.class,
                    () -> transactionService.depositToAccount(session, account, BigDecimal.valueOf(300)));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(500, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testDepositWithNegativeAmount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.depositToAccount(session, account, BigDecimal.valueOf(-100)));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(500, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testDepositWithZeroAmount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.depositToAccount(session, account, BigDecimal.ZERO));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(500, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testSuccessfulWithdrawal() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(account);
            session.getTransaction().commit();

            transactionService.withdrawFromAccount(session, account, BigDecimal.valueOf(200));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(300, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testWithdrawFromClosedAccount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            account.setStatus(AccountStatus.CLOSED);
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.withdrawFromAccount(session, account, BigDecimal.valueOf(200)));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(500, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testWithdrawWithInsufficientFunds() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(100));
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.withdrawFromAccount(session, account, BigDecimal.valueOf(200)));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(100, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testWithdrawWithNegativeAmount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.withdrawFromAccount(session, account, BigDecimal.valueOf(-100)));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(500, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testWithdrawWithZeroAmount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(BankingOperationException.class,
                    () -> transactionService.withdrawFromAccount(session, account, BigDecimal.ZERO));

            session.beginTransaction();
            Account updatedAccount = session.find(Account.class, account.getId());
            assertEquals(500, updatedAccount.getBalance().intValue());
        }
    }

    @Test
    void testTransactionRecordAfterDeposit() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(account);
            session.getTransaction().commit();

            transactionService.depositToAccount(session, account, BigDecimal.valueOf(300));

            session.beginTransaction();
            List<TransactionRecord> transactions = transactionService.getTransactions(session,0, 10);
            assertFalse(transactions.isEmpty());
            TransactionRecord record = transactions.getLast();
            assertEquals(TransactionType.DEPOSIT, record.getType());
            assertEquals(BigDecimal.valueOf(300), record.getAmount());
            assertEquals(account.getId(), record.getToAccount().getId());
        }
    }

    @Test
    void testTransactionRecordAfterWithdrawal() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, BigDecimal.valueOf(500));
            session.persist(account);
            session.getTransaction().commit();

            transactionService.withdrawFromAccount(session, account, BigDecimal.valueOf(200));

            session.beginTransaction();
            List<TransactionRecord> transactions = transactionService.getTransactions(session, 0, 10);
            assertFalse(transactions.isEmpty());
            TransactionRecord record = transactions.getLast();
            assertEquals(TransactionType.WITHDRAWAL, record.getType());
            assertEquals(BigDecimal.valueOf(200), record.getAmount());
            assertEquals(account.getId(), record.getFromAccount().getId());
        }
    }

    private Client createTestClient(Session session) {
        Client client = Client.builder()
                .fullName("Тестовый Клиент")
                .phoneNumber(TestDataGenerator.generateUniquePhone())
                .inn(TestDataGenerator.generateUniqueINN())
                .address("ул. Тестовая, 1")
                .build();

        session.persist(client);
        session.flush();
        return client;
    }

    private Account createTestAccount(Client client) {
        return createTestAccount(client, BigDecimal.TEN, Currency.USD);
    }

    private Account createTestAccount(Client client, BigDecimal balance) {
        return createTestAccount(client, balance, Currency.USD);
    }

    private Account createTestAccount(Client client, BigDecimal balance, Currency currency) {
        return Account.builder()
                .accountNumber(TestDataGenerator.generateAccountNumber())
                .bik(TestDataGenerator.generateBIK())
                .balance(balance)
                .currency(currency)
                .status(AccountStatus.OPEN)
                .client(client)
                .build();
    }

    private Session getNewSession() {
        Session session = entityManagerFactory.createEntityManager().unwrap(Session.class);
        session.beginTransaction();
        return session;
    }
}