package org.example.ClassiUtente;

public class Account {
    private String name;
    private double balance;
    private Transaction lastTransaction;

    public Account(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Transaction getLastTransaction() {
        return lastTransaction;
    }

    public void setLastTransaction(Transaction lastTransaction) {
        this.lastTransaction = lastTransaction;
    }

    // Method to perform a transaction between two accounts
    public boolean transfer(Account destination, double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            destination.balance += amount;
            return true;
        }
        return false;
    }
}
