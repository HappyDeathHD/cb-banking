package ru.hd.exception;

public class SameAccountTransferException extends BankingOperationException {
    public SameAccountTransferException(Long accountId) {
        super("Перевод на тот же счет невозможен: accountId=" + accountId);
    }
}