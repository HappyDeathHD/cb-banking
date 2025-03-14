package ru.hd.service;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.PropertyValueException;
import org.hibernate.Session;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hd.exception.*;
import ru.hd.jpa.Client;
import testutil.TestDataGenerator;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    private static EntityManagerFactory entityManagerFactory;
    private ClientService clientService;

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
        clientService = new ClientService();
    }

    private Session getNewSession() {
        return entityManagerFactory.createEntityManager().unwrap(Session.class);
    }

    @Test
    void testSuccessfulClientCreation() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = Client.builder()
                    .fullName("Иван Иванов")
                    .phoneNumber(TestDataGenerator.generateUniquePhone())
                    .inn(TestDataGenerator.generateUniqueINN())
                    .address("ул. Тестовая, 1")
                    .build();

            Client createdClient = clientService.createClient(session, client, new byte[]{});

            assertNotNull(createdClient.getId());
            assertEquals(client.getFullName(), createdClient.getFullName());
            assertEquals(client.getPhoneNumber(), createdClient.getPhoneNumber());
            assertEquals(client.getInn(), createdClient.getInn());
            assertEquals(client.getAddress(), createdClient.getAddress());
        }
    }

    @Test
    void testSamePhoneNumber() throws BankingOperationException {
        try (Session session = getNewSession()) {
            String phone = TestDataGenerator.generateUniquePhone();
            String inn1 = TestDataGenerator.generateUniqueINN();
            String inn2 = TestDataGenerator.generateUniqueINN();

            Client client1 = Client.builder()
                    .fullName("Иван Иванов")
                    .phoneNumber(phone)
                    .inn(inn1)
                    .address("ул. Тестовая, 1")
                    .build();

            clientService.createClient(session, client1, new byte[]{});

            Client client2 = Client.builder()
                    .fullName("Петр Петров")
                    .phoneNumber(phone) // Дублируем номер
                    .inn(inn2)
                    .address("ул. Тестовая, 2")
                    .build();

            assertThrows(InvalidPhoneNumberException.class,
                    () -> clientService.createClient(session, client2, new byte[]{}));
        }
    }

    @Test
    void testInvalidPhoneNumber() {
        try (Session session = getNewSession()) {
            Client client = Client.builder()
                    .fullName("Иван Иванов")
                    .phoneNumber("INVALID_PHONE")
                    .inn(TestDataGenerator.generateUniqueINN())
                    .address("ул. Тестовая, 1")
                    .build();

            assertThrows(InvalidPhoneNumberException.class,
                    () -> clientService.createClient(session, client, new byte[]{}));
        }
    }

    @Test
    void testSameINN() throws BankingOperationException {
        try (Session session = getNewSession()) {
            String inn = TestDataGenerator.generateUniqueINN();
            String phone1 = TestDataGenerator.generateUniquePhone();
            String phone2 = TestDataGenerator.generateUniquePhone();

            Client client1 = Client.builder()
                    .fullName("Иван Иванов")
                    .phoneNumber(phone1)
                    .inn(inn)
                    .address("ул. Тестовая, 1")
                    .build();

            clientService.createClient(session, client1, new byte[]{});

            Client client2 = Client.builder()
                    .fullName("Петр Петров")
                    .phoneNumber(phone2)
                    .inn(inn) // Дублируем ИНН
                    .address("ул. Тестовая, 2")
                    .build();

            assertThrows(InvalidTaxIdentifierException.class,
                    () -> clientService.createClient(session, client2, new byte[]{}));
        }
    }

    @Test
    void testInvalidINN() {
        try (Session session = getNewSession()) {
            Client client = Client.builder()
                    .fullName("Иван Иванов")
                    .phoneNumber(TestDataGenerator.generateUniquePhone())
                    .inn("INVALID_INN")
                    .address("ул. Тестовая, 1")
                    .build();

            assertThrows(InvalidTaxIdentifierException.class,
                    () -> clientService.createClient(session, client, new byte[]{}));
        }
    }

    @Test
    void testNullFullName() {
        try (Session session = getNewSession()) {
            Client client = Client.builder()
                    .fullName(null)
                    .phoneNumber(TestDataGenerator.generateUniquePhone())
                    .inn(TestDataGenerator.generateUniqueINN())
                    .address("ул. Тестовая, 1")
                    .build();

            assertThrows(PropertyValueException.class,
                    () -> clientService.createClient(session, client, new byte[]{}));
        }
    }

    @Test
    void testNullPhoneNumber() {
        try (Session session = getNewSession()) {
            Client client = Client.builder()
                    .fullName("Иван Иванов")
                    .phoneNumber(null)
                    .inn(TestDataGenerator.generateUniqueINN())
                    .address("ул. Тестовая, 1")
                    .build();

            assertThrows(InvalidPhoneNumberException.class,
                    () -> clientService.createClient(session, client, new byte[]{}));
        }
    }

    @Test
    void testNullINN() {
        try (Session session = getNewSession()) {
            Client client = Client.builder()
                    .fullName("Иван Иванов")
                    .phoneNumber(TestDataGenerator.generateUniquePhone())
                    .inn(null)
                    .address("ул. Тестовая, 1")
                    .build();

            assertThrows(InvalidTaxIdentifierException.class,
                    () -> clientService.createClient(session, client, new byte[]{}));
        }
    }

    @Test
    void testNullAddress() {
        try (Session session = getNewSession()) {
            Client client = Client.builder()
                    .fullName("Иван Иванов")
                    .phoneNumber(TestDataGenerator.generateUniquePhone())
                    .inn(TestDataGenerator.generateUniqueINN())
                    .address(null)
                    .build();

            assertThrows(PropertyValueException.class,
                    () -> clientService.createClient(session, client, new byte[]{}));
        }
    }

    @Test
    void testSuccessfulClientUpdate() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);

            Client updatedClient = new Client();
            updatedClient.setId(client.getId());
            updatedClient.setFullName("Обновленное Имя");
            updatedClient.setPhoneNumber(TestDataGenerator.generateUniquePhone());
            updatedClient.setInn(TestDataGenerator.generateUniqueINN());
            updatedClient.setAddress("ул. Обновленная, 1");

            Client result = clientService.updateClientWithPassportScan(session, updatedClient, new byte[]{});

            assertNotNull(result);
            assertEquals(updatedClient.getFullName(), result.getFullName());
            assertEquals(updatedClient.getPhoneNumber(), result.getPhoneNumber());
            assertEquals(updatedClient.getInn(), result.getInn());
            assertEquals(updatedClient.getAddress(), result.getAddress());
        }
    }

    @Test
    void testUpdateWithDuplicatePhoneNumber() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client1 = createTestClient(session);
            Client client2 = createTestClient(session);

            Client updatedClient = new Client();
            updatedClient.setId(client2.getId());
            updatedClient.setFullName("Обновленное Имя");
            updatedClient.setPhoneNumber(client1.getPhoneNumber()); // Дублируем номер
            updatedClient.setInn(client2.getInn());
            updatedClient.setAddress("ул. Обновленная, 1");

            assertThrows(InvalidPhoneNumberException.class,
                    () -> clientService.updateClientWithPassportScan(session, updatedClient, new byte[]{}));
        }
    }

    @Test
    void testUpdateWithDuplicateINN() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client1 = createTestClient(session);
            Client client2 = createTestClient(session);

            Client updatedClient = new Client();
            updatedClient.setId(client2.getId());
            updatedClient.setFullName("Обновленное Имя");
            updatedClient.setPhoneNumber(client2.getPhoneNumber());
            updatedClient.setInn(client1.getInn()); // Дублируем ИНН
            updatedClient.setAddress("ул. Обновленная, 1");

            assertThrows(InvalidTaxIdentifierException.class,
                    () -> clientService.updateClientWithPassportScan(session, updatedClient, new byte[]{}));
        }
    }

    @Test
    void testUpdateWithInvalidPhoneNumber() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);

            Client updatedClient = new Client();
            updatedClient.setId(client.getId());
            updatedClient.setFullName("Обновленное Имя");
            updatedClient.setPhoneNumber("INVALID_PHONE");
            updatedClient.setInn(client.getInn());
            updatedClient.setAddress("ул. Обновленная, 1");

            assertThrows(InvalidPhoneNumberException.class,
                    () -> clientService.updateClientWithPassportScan(session, updatedClient, new byte[]{}));
        }
    }

    @Test
    void testUpdateWithInvalidINN() throws BankingOperationException {
        try (Session session = getNewSession()) {
            Client client = createTestClient(session);

            Client updatedClient = new Client();
            updatedClient.setId(client.getId());
            updatedClient.setFullName("Обновленное Имя");
            updatedClient.setPhoneNumber(client.getPhoneNumber());
            updatedClient.setInn("INVALID_INN");
            updatedClient.setAddress("ул. Обновленная, 1");

            assertThrows(InvalidTaxIdentifierException.class,
                    () -> clientService.updateClientWithPassportScan(session, updatedClient, new byte[]{}));
        }
    }

    private Client createTestClient(Session session) throws BankingOperationException {
        Client client = Client.builder()
                .fullName("Иван Иванов")
                .phoneNumber(TestDataGenerator.generateUniquePhone())
                .inn(TestDataGenerator.generateUniqueINN())
                .address("ул. Тестовая, 1")
                .build();

        return clientService.createClient(session, client, new byte[]{});
    }
}