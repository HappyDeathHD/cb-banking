package ru.hd.service;

import jakarta.persistence.RollbackException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hd.exception.BankingOperationException;
import ru.hd.exception.ClientNotFoundException;
import ru.hd.exception.InvalidPhoneNumberException;
import ru.hd.exception.InvalidTaxIdentifierException;
import ru.hd.jpa.Client;
import ru.hd.jpa.PassportScan;

import java.util.List;

import static ru.hd.util.ValidationPattern.INN;
import static ru.hd.util.ValidationPattern.PHONE;

public class ClientService extends SessionService {
    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);

    public Client createClient(Session session, Client client, byte[] fileBytes)
            throws BankingOperationException {
        validateSession(session);
        Transaction transaction = null;
        try {
            validatePhone(client.getPhoneNumber());
            validateINN(client.getInn());

            transaction = session.beginTransaction();
            if (fileBytes != null && fileBytes.length > 0) {
                PassportScan passportScan = new PassportScan();
                passportScan.setClient(client);
                client.setPassportScan(passportScan);
                passportScan.setScan(fileBytes);
            }
            session.persist(client);
            transaction.commit();
            logger.info("Клиент успешно создан: fullName={}, phone={}", client.getFullName(), client.getPhoneNumber());
            return client;
        } catch (ConstraintViolationException e) {
            handleConstraintViolation(e, client);
            throw e;
        } catch (Exception e) {
            rollbackSafely(transaction);
            logger.error("Ошибка при создании клиента: fullName={}, phone={}", client.getFullName(), client.getPhoneNumber(), e);
            throw e;
        }
    }

    public Client updateClientWithPassportScan(Session session, Client updatedClient, byte[] fileBytes) throws ClientNotFoundException, InvalidTaxIdentifierException, InvalidPhoneNumberException {
        validateSession(session);
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();

            Client existingClient = session.get(Client.class, updatedClient.getId());
            if (existingClient == null) {
                throw new ClientNotFoundException(updatedClient.getId());
            }
            if (!existingClient.getPhoneNumber().equals(updatedClient.getPhoneNumber())) {
                validatePhone(updatedClient.getPhoneNumber());
            }
            if (!existingClient.getInn().equals(updatedClient.getInn())) {
                validateINN(updatedClient.getInn());
            }

            existingClient.setFullName(updatedClient.getFullName());
            existingClient.setPhoneNumber(updatedClient.getPhoneNumber());
            existingClient.setInn(updatedClient.getInn());
            existingClient.setAddress(updatedClient.getAddress());

            if (fileBytes != null && fileBytes.length > 0) {
                PassportScan passportScan = existingClient.getPassportScan();
                if (passportScan == null) {
                    passportScan = new PassportScan();
                    passportScan.setClient(existingClient);
                    existingClient.setPassportScan(passportScan);
                }
                passportScan.setScan(fileBytes);
            }
            session.merge(existingClient);
            transaction.commit();
            logger.info("Клиент успешно обновлен: ID={}, fullName={}", existingClient.getId(), existingClient.getFullName());
            return existingClient;
        } catch (ConstraintViolationException e) {
            handleConstraintViolation(e, updatedClient);
            throw e;
        } catch (RollbackException e) {
            handleRollbackException(e, updatedClient);
            throw e;
        } catch (Exception e) {
            rollbackSafely(transaction);
            logger.error("Ошибка при обновлении клиента: ID={}, fullName={}",
                    updatedClient.getId(),
                    updatedClient.getFullName(), e);
            throw e;
        }
    }

    public List<Client> getClients(Session session, int offset, int limit) {
        validateSession(session);
        return session.createQuery("FROM Client", Client.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Client> getAllClients(Session session) {
        validateSession(session);
        return session.createQuery("FROM Client", Client.class).getResultList();
    }

    public int getTotalClientsCount(Session session) {
        validateSession(session);
        return session.createQuery("SELECT COUNT(c) FROM Client c", Long.class)
                .getSingleResult()
                .intValue();
    }

    public PassportScan initializePassportScan(Session session, Client client) {
        Client existingClient = session.createQuery("SELECT c FROM Client c LEFT JOIN FETCH c.passportScan WHERE c.id = :id",
                        Client.class)
                .setParameter("id", client.getId())
                .uniqueResult();
        return existingClient != null ? existingClient.getPassportScan() : null;
    }

    private void validatePhone(String phone) throws InvalidPhoneNumberException {
        if (phone == null || !PHONE.matcher(phone).matches()) {
            throw new InvalidPhoneNumberException("Неверный формат телефона: " + phone);
        }
    }

    private void validateINN(String inn) throws InvalidTaxIdentifierException {
        if (inn == null || !INN.matcher(inn).matches()) {
            throw new InvalidTaxIdentifierException("Неверный формат ИНН: " + inn);
        }
    }

    private void handleConstraintViolation(ConstraintViolationException e, Client client) throws InvalidTaxIdentifierException, InvalidPhoneNumberException {
        analyzeAndThrow(e.getMessage(), client);
    }

    private void handleRollbackException(RollbackException e, Client client) throws InvalidTaxIdentifierException, InvalidPhoneNumberException {
        if (e.getCause() != null) {
            analyzeAndThrow(e.getCause().getMessage(), client);
        }
    }

    private void analyzeAndThrow(String errorMessage, Client client) throws InvalidTaxIdentifierException, InvalidPhoneNumberException {
        if (errorMessage != null) {
            if (errorMessage.contains("PHONE_NUMBER NULLS")) {
                throw new InvalidPhoneNumberException("Номер телефона уже используется: " + client.getPhoneNumber());
            }
            if (errorMessage.contains("INN NULLS")) {
                throw new InvalidTaxIdentifierException("Пользователь с таким ИНН уже существует: " + client.getInn());
            }
        }
    }

    private void rollbackSafely(Transaction transaction) {
        if (transaction != null && transaction.isActive()) {
            try {
                transaction.rollback();
                logger.info("Транзакция успешно откачена");
            } catch (Exception ex) {
                logger.error("Ошибка при откате транзакции", ex);
            }
        }
    }
}