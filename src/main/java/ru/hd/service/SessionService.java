package ru.hd.service;

import org.hibernate.Session;

public abstract class SessionService {

    protected void validateSession(Session session) {
        if (session == null || !session.isOpen()) {
            throw new IllegalStateException("Сессия не открыта");
        }
    }
}
