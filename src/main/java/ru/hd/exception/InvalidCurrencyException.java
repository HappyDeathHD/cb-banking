package ru.hd.exception;

public class InvalidCurrencyException extends BankingOperationException {
    public InvalidCurrencyException(String message) {
        super(message);
    }
}