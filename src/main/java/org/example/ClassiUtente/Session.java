package org.example.ClassiUtente;

class Session {
    private final Account account1;
    private final Account account2;

    public Session(Account account1, Account account2) {
        this.account1 = account1;
        this.account2 = account2;
    }

    public boolean move(double amount) {
        synchronized (Server.getAccounts()) {
            if (account1.getBalance() >= amount) {
                account1.transfer(account2, amount);
                return true;
            }
            return false;
        }
    }


}
