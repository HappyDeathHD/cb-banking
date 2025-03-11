package ru.hd.model;

import lombok.Getter;

@Getter
public enum AccountStatus {
    OPEN("Активный"),
    CLOSED("Закрытый");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

}
