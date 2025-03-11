package ru.hd.exception;

public class InvalidPhoneNumberException extends BankingOperationException {
    public InvalidPhoneNumberException(String message) {
        super(message);
    }
}