package ru.hd.exception;

public class InvalidBankIdentifierCodeException extends BankingOperationException {
    public InvalidBankIdentifierCodeException(String bik) {
        super("Неправильный банковский идентификационный код: " + bik);
    }
}