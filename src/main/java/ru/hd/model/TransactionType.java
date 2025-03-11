package ru.hd.model;

import lombok.Getter;

@Getter
public enum TransactionType {
    DEPOSIT("Пополнение"),
    TRANSFER("Перевод"),
    WITHDRAWAL("Снятие");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

}
