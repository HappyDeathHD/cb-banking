package ru.hd.exception;

public class DuplicateAccountException extends BankingOperationException {
    public DuplicateAccountException(String accountNumber) {
        super("Счет с таким номером уже существует: " + accountNumber);
    }
}