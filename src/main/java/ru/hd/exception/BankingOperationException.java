package ru.hd.exception;

public abstract class BankingOperationException extends Exception {
    public BankingOperationException(String message) {
        super(message);
    }
}