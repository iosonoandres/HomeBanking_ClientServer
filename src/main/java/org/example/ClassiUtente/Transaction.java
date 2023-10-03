package org.example.ClassiUtente;

import java.time.LocalDate;
import java.util.Date;

public class Transaction {
    private double amount;

    //mettere stringa in date
    private String data;

    // le due variabili di tipo Account sono segnate con "transient" per non essere incluse nella serializzazione JSON
    private transient Account sender;
    private transient Account receiver;

    private String nomeSender;

    private String nomeReceiver;

    public Transaction(double amount, String data, Account sender, Account receiver) {
        this.amount = amount;
        this.data = LocalDate.now().toString();
        this.sender = sender;
        this.receiver = receiver;
        nomeSender = sender.getName();
        nomeReceiver = receiver.getName();
    }

    public double getAmount() {
        return amount;
    }

    public String getDate() {
        return data;
    }

    public void setDate(String date) {
        this.data = date;
    }


    public Account getSender() {
        return sender;
    }

    public Account getReceiver() {
        return receiver;
    }

    public String getNomeReceiver() {
        return nomeReceiver;
    }

    public String getNomeSender() {
        return nomeSender;
    }

}
