package ru.hd.exception;

public class AccountNotFoundException extends BankingOperationException {
    public AccountNotFoundException(Long accountId) {
        super("Счет не найден accountId: " + accountId);
    }

    public AccountNotFoundException(String accountNumber) {
        super("Счет не найден accountNumber: " + accountNumber);
    }
}