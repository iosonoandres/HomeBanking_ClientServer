package org.example.ClassiUtente;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {
    private static final LockManager instance = new LockManager();

    private final Map<Account, Lock> accountLocks;

    private LockManager() {
        accountLocks = new HashMap<>();
    }

    public static LockManager getInstance() {
        return instance;
    }

    public Lock getLockForAccount(Account account) {
        synchronized (accountLocks) {
            return accountLocks.computeIfAbsent(account, acc -> new ReentrantLock(true));
        }
    }
}