package ru.hd.exception;

public class InvalidAmountException extends BankingOperationException {
    public InvalidAmountException(String message) {
        super(message);
    }
}