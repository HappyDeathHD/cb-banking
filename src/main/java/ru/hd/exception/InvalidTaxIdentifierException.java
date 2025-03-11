package ru.hd.exception;

public class InvalidTaxIdentifierException extends BankingOperationException {
    public InvalidTaxIdentifierException(String message) {
        super(message);
    }
}