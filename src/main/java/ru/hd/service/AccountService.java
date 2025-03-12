package ru.hd.service;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hd.exception.*;
import ru.hd.jpa.Account;
import ru.hd.model.AccountStatus;

import java.math.BigDecimal;
import java.util.List;

import static ru.hd.util.ValidationPattern.ACCOUNT_NUMBER;
import static ru.hd.util.ValidationPattern.BIK;

public class AccountService extends SessionService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    public Account createAccount(Session session, Account account)
            throws BankingOperationException {
        validateSession(session);

        Transaction transaction = null;
        try {
            validateBik(account.getBik());
            validateAccountNumber(account.getAccountNumber());
            checkAccountNumberUniqueness(session, account.getAccountNumber());

            transaction = session.beginTransaction();
            Account newAccount = buildNewAccount(account);
            session.persist(newAccount);
            transaction.commit();

            logger.info("Счет успешно создан: accountNumber={}, clientId={}",
                    account.getAccountNumber(), account.getClient().getId());
            return newAccount;
        } catch (BankingOperationException e) {
            rollbackSafely(transaction);
            logError("Ошибка при создании счета", account, e);
            throw e;
        }
    }

    public Account updateAccount(Session session, Account updatedAccount)
            throws BankingOperationException {
        validateSession(session);

        Transaction transaction = null;
        try {
            Account existingAccount = getExistingAccount(session, updatedAccount.getId());
            validateAndUpdateFields(session, existingAccount, updatedAccount);

            transaction = session.beginTransaction();
            session.merge(existingAccount);
            transaction.commit();

            logger.info("Счет успешно обновлен: ID={}, accountNumber={}",
                    existingAccount.getId(), existingAccount.getAccountNumber());
            return existingAccount;
        } catch (BankingOperationException e) {
            rollbackSafely(transaction);
            logError("Ошибка при обновлении счета", updatedAccount, e);
            throw e;
        }
    }

    public void closeAccount(Session session, Account account) throws BankingOperationException {
        validateSession(session);

        Transaction transaction = null;
        try {
            Account managedAccount = session.get(Account.class, account.getId());

            if (!managedAccount.getStatus().equals(AccountStatus.OPEN)) {
                throw new InvalidAccountStatusException("Счет уже закрыт");
            }

            if (managedAccount.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                throw new NonZeroBalanceException();
            }

            transaction = session.beginTransaction();
            managedAccount.setStatus(AccountStatus.CLOSED);
            session.merge(managedAccount);
            transaction.commit();

            logger.info("Счет {} закрыт", managedAccount.getAccountNumber());
        } catch (Exception e) {
            rollbackSafely(transaction);
            logger.error("Ошибка закрытия счета ID {}", account.getId(), e);
            throw e;
        }
    }

    public Account findAccountByNumber(Session session, String accountNumber)
            throws AccountNotFoundException {
        validateSession(session);

        Account account = session.createQuery(
                        "FROM Account WHERE accountNumber = :number", Account.class)
                .setParameter("number", accountNumber)
                .uniqueResult();

        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }

        return account;
    }

    public List<Account> getAccounts(Session session, int offset, int limit) {
        validateSession(session);
            return session.createQuery(
                            "SELECT a FROM Account a JOIN FETCH a.client", Account.class)
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .getResultList();
    }

    public List<Account> getAllAccounts(Session session) {
        validateSession(session);
            return session.createQuery(
                            "SELECT a FROM Account a JOIN FETCH a.client", Account.class)
                    .getResultList();
    }

    public int getTotalAccountsCount(Session session) {
        validateSession(session);
            return session.createQuery("SELECT COUNT(a) FROM Account a", Long.class)
                    .getSingleResult()
                    .intValue();
    }

    private void checkAccountNumberUniqueness(Session session, String accountNumber)
            throws DuplicateAccountException {
        validateSession(session);
        if (isAccountNumberExists(session, accountNumber)) {
            throw new DuplicateAccountException(accountNumber);
        }
    }

    private Account buildNewAccount(Account account) {
        return Account.builder()
                .accountNumber(account.getAccountNumber())
                .bik(account.getBik())
                .currency(account.getCurrency())
                .client(account.getClient())
                .status(AccountStatus.OPEN)
                .balance(BigDecimal.ZERO)
                .build();
    }

    private Account getExistingAccount(Session session, Long accountId)
            throws AccountNotFoundException {
        Account existingAccount = session.get(Account.class, accountId);
        if (existingAccount == null) {
            throw new AccountNotFoundException(accountId);
        }
        return existingAccount;
    }

    private void validateAndUpdateFields(Session session, Account existing, Account updated)
            throws BankingOperationException {
        if (updated.getClient() == null) {
            throw new IllegalStateException("Клиент не может быть null");
        }
        if (existing.getBalance().compareTo(BigDecimal.ZERO) != 0 &&
                !existing.getCurrency().equals(updated.getCurrency())) {
            throw new InvalidCurrencyException("Нельзя менять валюту на не пустом счету");
        }
        validateBik(updated.getBik());
        validateAccountNumber(updated.getAccountNumber());

        if (!existing.getBik().equals(updated.getBik())) {
            existing.setBik(updated.getBik());
        }

        if (!existing.getAccountNumber().equals(updated.getAccountNumber())) {
            checkAccountNumberUniqueness(session, updated.getAccountNumber(), existing.getId());
            existing.setAccountNumber(updated.getAccountNumber());
        }

        existing.setCurrency(updated.getCurrency());
        existing.setClient(updated.getClient());
    }

    private void checkAccountNumberUniqueness(Session session, String accountNumber, Long currentId)
            throws DuplicateAccountException {
        if (isAccountNumberExistsExcludingCurrent(session, accountNumber, currentId)) {
            throw new DuplicateAccountException(accountNumber);
        }
    }

    private boolean isAccountNumberExists(Session session, String accountNumber) {
        return session.createQuery(
                        "SELECT COUNT(a.id) FROM Account a WHERE a.accountNumber = :number", Long.class)
                .setParameter("number", accountNumber)
                .uniqueResult() > 0;
    }

    private boolean isAccountNumberExistsExcludingCurrent(Session session, String accountNumber, Long currentId) {
        return session.createQuery(
                        "SELECT COUNT(a.id) FROM Account a WHERE a.accountNumber = :number AND a.id != :id", Long.class)
                .setParameter("number", accountNumber)
                .setParameter("id", currentId)
                .uniqueResult() > 0;
    }

    private void validateAccountNumber(String accountNumber)
            throws InvalidAccountNumberException {
        if (accountNumber == null || !ACCOUNT_NUMBER.matcher(accountNumber).matches()) {
            throw new InvalidAccountNumberException(accountNumber);
        }
    }

    private void validateBik(String bik)
            throws InvalidBankIdentifierCodeException {
        if (bik == null || !BIK.matcher(bik).matches()) {
            throw new InvalidBankIdentifierCodeException(bik);
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

    private void logError(String message, Account account, Exception e) {
        logger.error("{}: accountNumber={}, clientId={}",
                message, account.getAccountNumber(), account.getClient().getId(), e);
    }
}