package ru.hd.service;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hd.exception.*;
import ru.hd.jpa.Account;
import ru.hd.jpa.TransactionRecord;
import ru.hd.model.AccountStatus;
import ru.hd.model.TransactionStatus;
import ru.hd.model.TransactionType;

import java.math.BigDecimal;
import java.util.List;

public class TransactionService extends SessionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public void transfer(Session session, Long fromAccountId,
                         Long toAccountId, BigDecimal amount)
            throws BankingOperationException {
        validateSession(session);
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();

            Account from = getValidAccount(session, fromAccountId);
            Account to = getValidAccount(session, toAccountId);

            validateTransfer(from, to, amount);

            executeTransfer(from, to, amount);
            recordTransaction(session, from, to, amount);

            transaction.commit();
            logger.info("Перевод успешно выполнен: fromAccountId={}, toAccountId={}, amount={}",
                    fromAccountId, toAccountId, amount);
        } catch (Exception e) {
            rollbackSafely(transaction);
            logger.error("Ошибка при выполнении перевода: fromAccountId={}, toAccountId={}, amount={}",
                    fromAccountId, toAccountId, amount, e);
            throw e;
        }
    }

    public void depositToAccount(Session session, Account account, BigDecimal amount)
            throws BankingOperationException {
        validateSession(session);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Неверная сумма пополнения: " + amount);
        }
        if (AccountStatus.CLOSED.equals(account.getStatus())) {
            throw new InvalidAccountStatusException("Нельзя пополнять закрытый счет");
        }

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();

            Account managedAccount = session.get(Account.class, account.getId());
            if (managedAccount == null) {
                throw new AccountNotFoundException(account.getId());
            }

            managedAccount.setBalance(managedAccount.getBalance().add(amount));
            session.merge(managedAccount);

            recordDepositTransaction(session, managedAccount, amount);

            transaction.commit();

            logger.info("Пополнение счета ID {}: +{}", managedAccount.getId(), amount);
        } catch (Exception e) {
            rollbackSafely(transaction);
            logger.error("Ошибка пополнения счета ID {}", account.getId(), e);
            throw e;
        }
    }

    public void withdrawFromAccount(Session session, Account account, BigDecimal amount)
            throws BankingOperationException {
        validateSession(session);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Неверная сумма снятия: " + amount);
        }

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();

            Account managedAccount = session.get(Account.class, account.getId());
            if (managedAccount == null) {
                throw new AccountNotFoundException(account.getId());
            }

            if (managedAccount.getStatus() != AccountStatus.OPEN) {
                throw new AccountClosedException(account.getId(), "Счет закрыт");
            }

            if (managedAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(account.getId(), "Недостаточно средств на счете");
            }

            managedAccount.setBalance(managedAccount.getBalance().subtract(amount));
            session.merge(managedAccount);

            recordWithdrawTransaction(session, managedAccount, amount);

            transaction.commit();

            logger.info("Снятие средств со счета ID {}: -{}", managedAccount.getId(), amount);
        } catch (Exception e) {
            rollbackSafely(transaction);
            logger.error("Ошибка снятия средств со счета ID {}", account.getId(), e);
            throw e;
        }
    }

    private void recordWithdrawTransaction(Session session, Account fromAccount, BigDecimal amount) {
        TransactionRecord transactionRecord = createWithdrawTransactionRecord(fromAccount, amount);
        session.persist(transactionRecord);
        logger.debug("Запись снятия создана: fromAccountId={}, amount={}", fromAccount.getId(), amount);
    }

    private TransactionRecord createWithdrawTransactionRecord(Account fromAccount, BigDecimal amount) {
        return TransactionRecord.builder()
                .amount(amount)
                .type(TransactionType.WITHDRAWAL)
                .fromAccount(fromAccount)
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    public List<TransactionRecord> getTransactions(Session session, int offset, int limit) {
            return session.createQuery(
                            "SELECT t FROM TransactionRecord t " +
                                    "LEFT JOIN FETCH t.fromAccount " +
                                    "LEFT JOIN FETCH t.toAccount", TransactionRecord.class)
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .getResultList();
    }

    public List<TransactionRecord> getAllTransactions(Session session) {
            return session.createQuery(
                            "SELECT t FROM TransactionRecord t " +
                                    "LEFT JOIN FETCH t.fromAccount " +
                                    "LEFT JOIN FETCH t.toAccount", TransactionRecord.class)
                    .getResultList();
    }

    public int getTotalTransactionsCount(Session session) {
            return session.createQuery("SELECT COUNT(t) FROM TransactionRecord t", Long.class)
                    .getSingleResult()
                    .intValue();
    }

    private Account getValidAccount(Session session, Long accountId)
            throws AccountNotFoundException, AccountClosedException {
        Account account = session.get(Account.class, accountId, LockMode.PESSIMISTIC_WRITE);
        if (account == null) {
            throw new AccountNotFoundException(accountId);
        }
        if (account.getStatus() != AccountStatus.OPEN) {
            throw new AccountClosedException(accountId);
        }
        return account;
    }

    private void validateTransfer(Account from, Account to, BigDecimal amount) throws BankingOperationException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Сумма перевода должна быть положительной");
        }

        if (from.getId().equals(to.getId())) {
            throw new SameAccountTransferException(from.getId());
        }

        if (from.getStatus() != AccountStatus.OPEN) {
            throw new AccountClosedException(from.getId(), "Счет отправителя закрыт");
        }
        if (to.getStatus() != AccountStatus.OPEN) {
            throw new AccountClosedException(to.getId(), "Счет получателя закрыт");
        }

        if (!from.getCurrency().equals(to.getCurrency())) {
            throw new InvalidCurrencyException(
                    "Несоответствие валют: " +
                            from.getCurrency() + " → " + to.getCurrency()
            );
        }

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(from.getId(), "Недостаточно средств на счете отправителя");
        }
    }

    private void executeTransfer(Account from, Account to, BigDecimal amount) {
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        logger.debug("Баланс обновлен: fromAccountId={}, newBalance={}; toAccountId={}, newBalance={}",
                from.getId(), from.getBalance(), to.getId(), to.getBalance());
    }

    private void recordTransaction(Session session, Account from, Account to, BigDecimal amount) {
        TransactionRecord transactionRecord = createTransactionRecord(from, to, amount);
        session.persist(transactionRecord);
        logger.debug("Запись транзакции создана: fromAccountId={}, toAccountId={}, amount={}",
                from.getId(), to.getId(), amount);
    }

    private TransactionRecord createTransactionRecord(Account from, Account to, BigDecimal amount) {
        return TransactionRecord.builder()
                .amount(amount)
                .type(TransactionType.TRANSFER)
                .fromAccount(from)
                .toAccount(to)
                .status(TransactionStatus.COMPLETED)
                .build();
    }

    private void recordDepositTransaction(Session session, Account toAccount, BigDecimal amount) {
        TransactionRecord transactionRecord = createDepositTransactionRecord(toAccount, amount);
        session.persist(transactionRecord);
        logger.debug("Запись депозита создана: toAccountId={}, amount={}", toAccount.getId(), amount);
    }

    private TransactionRecord createDepositTransactionRecord(Account toAccount, BigDecimal amount) {
        return TransactionRecord.builder()
                .amount(amount)
                .type(TransactionType.DEPOSIT)
                .toAccount(toAccount)
                .status(TransactionStatus.COMPLETED)
                .build();
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