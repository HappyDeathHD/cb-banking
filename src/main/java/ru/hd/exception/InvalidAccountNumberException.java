package ru.hd.exception;

public class InvalidAccountNumberException extends BankingOperationException {
    public InvalidAccountNumberException(String accountNumber) {
        super("Неправильный номер счета: " + accountNumber);
    }
}