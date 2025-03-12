package ru.hd.exception;

public class ClientNotFoundException extends BankingOperationException {
    public ClientNotFoundException(Long clientId) {
        super("Клиент не найден clientId: " + clientId);
    }
}