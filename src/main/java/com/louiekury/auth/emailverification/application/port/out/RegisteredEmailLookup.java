package com.louiekury.auth.emailverification.application.port.out;

public interface RegisteredEmailLookup {
    boolean exists(String email);
}
