package ru.hd.exception;

public class AccountClosedException extends BankingOperationException {
    public AccountClosedException(Long accountId) {
        super("Счет закрыт: " + accountId);
    }

    public AccountClosedException(Long accountId, String message) {
        super("Счет с ID " + accountId + " закрыт: " + message);
    }
}