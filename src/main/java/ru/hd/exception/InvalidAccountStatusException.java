package ru.hd.exception;

public class InvalidAccountStatusException extends BankingOperationException {
    public InvalidAccountStatusException(String message) {
        super(message);
    }
}