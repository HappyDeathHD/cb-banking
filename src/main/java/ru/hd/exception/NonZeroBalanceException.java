package ru.hd.exception;

public class NonZeroBalanceException extends BankingOperationException {
    public NonZeroBalanceException() {
        super("Баланс не равен нулю");
    }
}