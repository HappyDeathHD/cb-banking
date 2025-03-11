package ru.hd.service;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hd.exception.*;
import ru.hd.jpa.Account;
import ru.hd.jpa.Client;
import ru.hd.model.AccountStatus;
import ru.hd.model.Currency;
import testutil.TestDataGenerator;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static EntityManagerFactory entityManagerFactory;
    private AccountService accountService;

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
        accountService = new AccountService();
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
    void testSuccessfulAccountCreation() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account accountData = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            session.getTransaction().commit();

            Account account = accountService.createAccount(session, accountData);

            assertNotNull(account.getId());
            assertEquals(accountData.getAccountNumber(), account.getAccountNumber());
            assertEquals(accountData.getBik(), account.getBik());
            assertEquals(Currency.RUB, account.getCurrency());
            assertEquals(BigDecimal.ZERO, account.getBalance());
            assertEquals(AccountStatus.OPEN, account.getStatus());
            assertEquals(client.getId(), account.getClient().getId());
        }
    }

    @Test
    void testDuplicateAccountNumber() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account firstAccount = createTestAccount(client, "01234567890123456789");
            session.getTransaction().commit();

            accountService.createAccount(session, firstAccount);

            Account duplicateAccount = createTestAccount(client, "01234567890123456789");
            assertThrows(DuplicateAccountException.class,
                    () -> accountService.createAccount(session, duplicateAccount));
        }
    }

    @Test
    void testInvalidAccountNumber() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account invalidAccount = createTestAccount(client, "INVALID");

            assertThrows(InvalidAccountNumberException.class,
                    () -> accountService.createAccount(session, invalidAccount));
        }
    }

    @Test
    void testInvalidBIK() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account invalidAccount = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            invalidAccount.setBik("INVALID");

            assertThrows(InvalidBankIdentifierCodeException.class,
                    () -> accountService.createAccount(session, invalidAccount));
        }
    }

    @Test
    void testNullClient() {
        try (Session session = getNewSession()) {
            Account account = createTestAccount(null, TestDataGenerator.generateAccountNumber());

            assertThrows(IllegalStateException.class,
                    () -> accountService.createAccount(session, account));
        }
    }

    @Test
    void testNullAccountNumber() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, null);

            assertThrows(InvalidAccountNumberException.class,
                    () -> accountService.createAccount(session, account));
        }
    }

    @Test
    void testNullBIK() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            account.setBik(null);

            assertThrows(InvalidBankIdentifierCodeException.class,
                    () -> accountService.createAccount(session, account));
        }
    }

    @Test
    void testNullCurrency() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            account.setCurrency(null);

            assertThrows(IllegalStateException.class,
                    () -> accountService.createAccount(session, account));
        }
    }

    @Test
    void testSuccessfulAccountUpdate() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            session.persist(account);
            session.getTransaction().commit();

            Account updatedAccount = new Account();
            updatedAccount.setId(account.getId());
            updatedAccount.setAccountNumber(TestDataGenerator.generateAccountNumber());
            updatedAccount.setBik(TestDataGenerator.generateBIK());
            updatedAccount.setCurrency(Currency.USD);
            updatedAccount.setClient(client);

            Account result = accountService.updateAccount(session, updatedAccount);

            assertNotNull(result);
            assertEquals(updatedAccount.getAccountNumber(), result.getAccountNumber());
            assertEquals(updatedAccount.getBik(), result.getBik());
            assertEquals(Currency.USD, result.getCurrency());
        }
    }

    @Test
    void testUpdateWithDuplicateAccountNumber() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account firstAccount = createTestAccount(client, "12345678901234567890");
            Account secondAccount = createTestAccount(client, "98765432109876543210");
            session.persist(firstAccount);
            session.persist(secondAccount);
            session.getTransaction().commit();

            // Пытаемся обновить второй счет на дублирующий номер первого
            Account updatedAccount = new Account();
            updatedAccount.setId(secondAccount.getId());
            updatedAccount.setAccountNumber("12345678901234567890");
            updatedAccount.setBik(secondAccount.getBik());
            updatedAccount.setCurrency(secondAccount.getCurrency());
            updatedAccount.setClient(client);

            assertThrows(DuplicateAccountException.class,
                    () -> accountService.updateAccount(session, updatedAccount));
        }
    }

    @Test
    void testUpdateInvalidAccountNumber() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            session.persist(account);
            session.getTransaction().commit();

            Account updatedAccount = new Account();
            updatedAccount.setId(account.getId());
            updatedAccount.setAccountNumber("INVALID");
            updatedAccount.setClient(client);
            updatedAccount.setBik(account.getBik());

            assertThrows(InvalidAccountNumberException.class,
                    () -> accountService.updateAccount(session, updatedAccount));
        }
    }

    @Test
    void testSuccessfulAccountClosure() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            session.persist(account);
            session.getTransaction().commit();

            accountService.closeAccount(session, account);

            Account closedAccount = session.get(Account.class, account.getId());
            assertEquals(AccountStatus.CLOSED, closedAccount.getStatus());
        }
    }

    @Test
    void testCloseAlreadyClosedAccount() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            account.setStatus(AccountStatus.CLOSED);
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(InvalidAccountStatusException.class,
                    () -> accountService.closeAccount(session, account));
        }
    }

    @Test
    void testCloseAccountWithNonZeroBalance() {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, TestDataGenerator.generateAccountNumber());
            account.setBalance(BigDecimal.TEN);
            session.persist(account);
            session.getTransaction().commit();

            assertThrows(NonZeroBalanceException.class,
                    () -> accountService.closeAccount(session, account));
        }
    }

    @Test
    void testFindExistingAccountByNumber() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);
            Account account = createTestAccount(client, "1234567890");
            session.persist(account);
            session.getTransaction().commit();

            Account foundAccount = accountService.findAccountByNumber(session, account.getAccountNumber());

            assertNotNull(foundAccount);
            assertEquals(account.getId(), foundAccount.getId());
        }
    }

    @Test
    void testFindNonExistingAccountByNumber() {
        try (Session session = getNewSession()) {
            assertThrows(AccountNotFoundException.class,
                    () -> accountService.findAccountByNumber(session, "99999999999999999999"));
        }
    }

    private Session getNewSession() {
        Session session = entityManagerFactory.createEntityManager().unwrap(Session.class);
        session.beginTransaction();
        return session;
    }

    private Client createTestClient(Session session) {
        Client client = Client.builder()
                .fullName("Тестовый Клиент")
                .phoneNumber(TestDataGenerator.generateUniquePhone())
                .inn(TestDataGenerator.generateUniqueINN())
                .address("ул. Тестовая, 1")
                .passportScanCopy("scan.jpg")
                .build();

        session.persist(client);
        session.flush();
        return client;
    }

    private Account createTestAccount(Client client, String accountNumber) {
        return Account.builder()
                .accountNumber(accountNumber)
                .bik(TestDataGenerator.generateBIK())
                .currency(Currency.RUB)
                .client(client)
                .build();
    }
}