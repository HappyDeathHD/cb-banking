package ru.hd.model;

import lombok.Getter;

@Getter
public enum TransactionStatus {
    PENDING("В процессе"),
    COMPLETED("Завершено"),
    FAILED("Ошибка");


    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }

}
