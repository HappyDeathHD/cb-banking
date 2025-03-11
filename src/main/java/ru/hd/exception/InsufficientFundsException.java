package ru.hd.exception;

public class InsufficientFundsException extends BankingOperationException {
    public InsufficientFundsException(Long accountId, String message) {
        super("Недостаточно средств на счете с ID " + accountId + ": " + message);
    }
}