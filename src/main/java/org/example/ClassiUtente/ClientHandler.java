package org.example.ClassiUtente;

import java.io.*;
import java.net.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.locks.Lock;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;
    private List<Transaction> sessionTransactions = new ArrayList<>();

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }


    @Override
    public void run() {
        try {
            inputReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outputWriter = new PrintWriter(clientSocket.getOutputStream(), true);

            System.out.println("New client thread started: " + Thread.currentThread());

            while (true) {
                String command = inputReader.readLine();
                String response = processCommand(command);
                sendMessage(response);
            }


        } catch (SocketException se) {
            System.out.println("SocketException: There was a problem with the socket: client disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendMessage(String message) {
        outputWriter.println(message + "\nEND_RESPONSE");
        outputWriter.flush();
    }

    private String processCommand(String command) throws IOException {
        String[] parts = command.split("\\s+");

        if (parts.length == 0) {
            return "Invalid command.";
        }

        String action = parts[0].toLowerCase();
        switch (action) {
            case "help":
                return showHelp();
            case "open":
                return handleOpenAccount(parts);
            case "close":
                return handleCloseAccount(parts);
            case "transfer":
                return handleTransfer(parts);
            case "transfer_i":
                return handleInteractiveTransfer(parts);
            case "list":
                return handleList();
            case "deposit":
                return handleDeposit(parts);
            case "withdraw":
                return handleWithdraw(parts);
            case "quit":
                return handleQuit();
            default:
                return "Invalid command.";
        }
    }

    private String handleWithdraw(String[] parts) {
        if (parts.length != 3) {
            return "Invalid command. Usage: withdraw <Account> <Amount>";
        }
        double withdraw;

        try {
            withdraw = Double.parseDouble(parts[2]);
            if (withdraw <= 0) {
                return "Invalid amount.";
            }

            Account acc;
            Lock lock;

            synchronized (Server.getAccounts()) {
                acc = findAccount(parts[1]);
                if (acc == null) {
                    return parts[1] + " not found";
                }

                lock = LockManager.getInstance().getLockForAccount(acc);
            }

            if (lock.tryLock()) {
                try {
                    if (acc.getBalance() >= withdraw) {
                        acc.setBalance(acc.getBalance() - withdraw);
                    } else {
                        withdraw = acc.getBalance();
                        acc.setBalance(0);
                    }
                    Server.saveAccounts();
                    return withdraw + " dollars withdrawn. " + parts[1] + "'s current balance: " + acc.getBalance();
                } finally {
                    lock.unlock();
                }
            } else {
                return "Cannot withdraw at the moment. Account is currently locked.";
            }
        } catch (NumberFormatException e) {
            return "Invalid amount.";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private String handleDeposit(String[] parts) {
        if (parts.length != 3) {
            return "Invalid command. Usage: deposit <Account> <Amount>";
        }
        double deposit;

        try {
            deposit = Double.parseDouble(parts[2]);
            if (deposit <= 0) {
                return "Invalid amount.";
            }

            Account acc;
            Lock lock;

            synchronized (Server.getAccounts()) {
                acc = findAccount(parts[1]);
                if (acc == null) {
                    return parts[1] + " not found";
                }

                lock = LockManager.getInstance().getLockForAccount(acc);
            }

            if (lock.tryLock()) {
                try {
                    acc.setBalance(acc.getBalance() + deposit);
                    Server.saveAccounts();
                    return "Operation successful. " + parts[1] + "'s current balance: " + acc.getBalance();
                } finally {
                    lock.unlock();
                }
            } else {
                return "Cannot deposit at the moment. Account is currently locked.";
            }
        } catch (NumberFormatException e) {
            return "Invalid amount.";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // InterruptedException temporanea per testare, dopo va tolto
    private String handleOpenAccount(String[] parts) throws IOException {
        if (parts.length != 3) {
            return "Invalid command. Usage: open <Account> <Amount>";
        }

        String accountName = parts[1];
        double initialBalance;

        try {
            initialBalance = Double.parseDouble(parts[2]);
            if (initialBalance < 0) {
                return "error opening " + accountName + ": initial balance cannot be negative";
            }
        } catch (NumberFormatException e) {
            return "Invalid amount.";
        }

        synchronized (Server.getAccounts()) {
            for (Account account : Server.getAccounts()) {
                if (account.getName().equals(accountName)) {
                    return "An account with the same name already exists.";
                }
            }

            Account newAccount = new Account(accountName, initialBalance);
            Server.getAccounts().add(newAccount);
            Server.saveAccounts();
        }

        return "Account '" + accountName + "' created successfully.";
    }

    //fixare concorrenza con lock nel close, se Parsa ha fatto altre modifiche, gestirla anche lí

    private String handleCloseAccount(String[] parts) throws IOException {

        if (parts.length != 2) {
            return "Invalid command. Usage: close <Account>";
        }

        String accountName = parts[1];

        Account closedAccount = findAccount(accountName);

        if (closedAccount == null) {
            return "Account " + accountName + " doesn't exist";
        }

        Lock lock = LockManager.getInstance().getLockForAccount(closedAccount);

        //si cerca di acquisire il lock
        boolean lockAcquired = lock.tryLock();

        if (!lockAcquired) {
            //se il lock è già acquisito si dice essere in uso
            return "Account " + accountName + " is currently in use and cannot be closed at the moment.";
        }

        try {
            if (Server.getAccounts().contains(closedAccount)) {
                Server.getAccounts().remove(closedAccount);
                Server.saveAccounts();
                return "Account " + accountName + " closed successfully.";
            }
        } finally {
            lock.unlock();
        }

        return "Account " + accountName + " doesn't exist";
    }


    private String handleTransfer(String[] parts) throws IOException {
        if (parts.length != 4) {
            return "Invalid command. Usage: transfer <Amount> <Account1> <Account2>";
        }

        double amount;
        String accountName1 = parts[2];
        String accountName2 = parts[3];

        try {
            amount = Double.parseDouble(parts[1]);
            if (amount < 0) {
                return "Cannot transfer negative sums";
            }
        } catch (NumberFormatException e) {
            return "Invalid amount.";
        }

        Account account1 = findAccount(accountName1);
        Account account2 = findAccount(accountName2);

        if (account1 == null || account2 == null) {
            return "One or both of the accounts does not exist.";
        } else if (account1 == account2) {
            return "Illegal to move money from and to the same account. Please use two different accounts";
        }

        Lock lock1 = LockManager.getInstance().getLockForAccount(account1);
        Lock lock2 = LockManager.getInstance().getLockForAccount(account2);

        // Acquire locks in a consistent order to avoid deadlocks
        if (account1.hashCode() < account2.hashCode()) {
            lock1.lock();
            lock2.lock();
        } else {
            lock2.lock();
            lock1.lock();
        }

        try {
            boolean transferResult = account1.transfer(account2, amount);
            if (transferResult) {


                String data = LocalDate.now().toString();
                Transaction transazione = new Transaction(amount, data, account1, account2);
                account1.setLastTransaction(transazione);
                account2.setLastTransaction(transazione);
                Server.saveAccounts();
                return "Transfer successful: $" + amount + " transferred from '" + accountName1 + "' to '" + accountName2 + "'.";
            } else {
                return "Transfer failed: Not enough balance or invalid accounts.";
            }
        } finally {
            lock1.unlock();
            lock2.unlock();
        }
    }

    private String handleInteractiveTransfer(String[] parts) throws IOException {
        if (parts.length != 3) {
            return "Invalid command. Usage: transfer_i <Account1> <Account2>";
        }

        String accountName1 = parts[1];
        String accountName2 = parts[2];


        Account account1 = findAccount(accountName1);
        Account account2 = findAccount(accountName2);

        if (account1 == null || account2 == null) {
            return "One or both of the accounts does not exist.";
        }

        // condition to check that the sender and recipient are not the same account
        if (accountName1.equalsIgnoreCase(accountName2)) {
            return "Cannot start interactive session to and from the same account. Please use two different accounts. ";
        }

        Lock lock1 = LockManager.getInstance().getLockForAccount(account1);
        Lock lock2 = LockManager.getInstance().getLockForAccount(account2);

        // Acquire locks in a consistent order to avoid deadlocks
        if (account1.hashCode() < account2.hashCode()) {
            lock1.lock();
            lock2.lock();
        } else {
            lock2.lock();
            lock1.lock();
        }

        try {

            Session session = new Session(account1, account2);

            outputWriter.println("Interactive transfer started between '" + accountName1 + "' and '" + accountName2 + "'.");
            outputWriter.println("You are now in an interactive session. You can use the following commands:");
            outputWriter.println(">:move <Amount> - Move money from '" + accountName1 + "' to '" + accountName2 + "'.");
            outputWriter.println(">:end - End the interactive session." + "\nEND_RESPONSE");
            outputWriter.flush(); // Invia i messaggi immediatamente al client


            // Creazione del thread per la sessione interattiva
            Thread sessionThread = new Thread(() -> {
                try {
                    while (true) {

                            String command = inputReader.readLine();

                            if (command.equals(":end")) {
                                // Invia il messaggio al client
                                outputWriter.flush(); // Invia immediatamente il messaggio al client
                                break;
                            } else if (command.startsWith(":move")) {
                                String date = LocalDate.now().toString();
                                String[] moveParts = command.split("\\s+");
                                if (moveParts.length != 2) {
                                    outputWriter.println("Invalid move command. Usage: :move <Amount>" + "\nEND_RESPONSE");
                                    outputWriter.flush();
                                    continue;
                                }

                                double amount;
                                try {
                                    amount = Double.parseDouble(moveParts[1]);
                                } catch (NumberFormatException e) {
                                    outputWriter.println("Invalid amount." + "\nEND_RESPONSE");
                                    outputWriter.flush();
                                    continue;
                                }
                                if (amount < 0) {
                                    outputWriter.println("Transfer amount cannot be negative." + "\nEND_RESPONSE");
                                    outputWriter.flush();
                                    continue;
                                }

                                boolean transferResult = session.move(amount);
                                if (transferResult) {
                                    // Aggiorna le transazioni nei conti coinvolti
                                    Transaction senderTransaction = new Transaction(-amount, date, account1, account2);
                                    Transaction receiverTransaction = new Transaction(amount, date, account1, account2);
                                    sessionTransactions.add(senderTransaction);
                                    sessionTransactions.add(receiverTransaction);
                                    account1.setLastTransaction(senderTransaction);
                                    account2.setLastTransaction(receiverTransaction);

                                    outputWriter.println("Transfer successful: $" + amount + " transferred from '" + accountName1 + "' to '" + accountName2 + "'." + "\nEND_RESPONSE");
                                    outputWriter.flush(); // Forza l'invio del messaggio
                                    System.out.flush();   // Svuota il buffer del terminale
                                } else {
                                    outputWriter.println("Transfer failed: Not enough balance or invalid accounts." + "\nEND_RESPONSE");
                                    outputWriter.flush(); // Forza l'invio del messaggio
                                    System.out.flush();   // Svuota il buffer del terminale
                                }
                            } else {
                                outputWriter.println("Invalid command. Available commands: :move, :end" + "\nEND_RESPONSE");
                            }
                        }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            sessionThread.start(); // Avvia il thread per la sessione interattiva
            // Attendi che il thread della sessione interattiva termini

            sessionThread.join();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock1.unlock();
            lock2.unlock();
        }


        //SALVA LE INFORMAZIONI DELLE TRANSAZIONI SOLO DOPO CHE LA SESSIONE VIENE CHIUSA,
        // QUESTO DOVREBBE RISOLVERE I PROBLEMI DI VISUALIZZAZIONE TESTUALI A TERMINALE

        if (!sessionTransactions.isEmpty()) {
            synchronized (Server.getAccounts()) {
                for (Transaction transaction : sessionTransactions) {
                    transaction.getSender().setLastTransaction(transaction);
                    transaction.getReceiver().setLastTransaction(transaction);
                }
                Server.saveAccounts();
            }
            sessionTransactions.clear();
        }

        return ("Interactive session ended");
    }



    private String showHelp() {

        String helpMessage = "Available commands:\n"
                + "open <Account> <Amount> - Open a new account with the specified name and initial amount.\n"
                + "close <Account> - Delete account with the specified name.\n"
                + "transfer <Amount> <Account1> <Account2> - Transfer money from Account1 to Account2.\n"
                + "transfer_i <Account1> <Account2> - Start an interactive transfer session between Account1 and Account2.\n"
                + "list - Show the list of all accounts and their balances.\n"
                + "quit - Close the connection with the server.\n"
                + "deposit <Account1> amount - Add some money to the account .\n"
                + "withdraw <Account1> amount - withdraw money from the account.\n"
                + "help - Show this help message.";


        outputWriter.print(helpMessage);
        outputWriter.flush();

        return "";
    }

    private String handleList() {
        synchronized (Server.getAccounts()) {
            for (Account account : Server.getAccounts()) {
                StringBuilder response = new StringBuilder();
                response.append("Account Name: ").append(account.getName());
                response.append(", Balance: $").append(account.getBalance());

                Transaction lastTransaction = account.getLastTransaction();
                if (lastTransaction != null) {
                    response.append(", Last Transaction: Amount: $")
                            .append(lastTransaction.getAmount())
                            .append(", Sender: ")
                            .append(lastTransaction.getSender() != null ? lastTransaction.getNomeSender() : lastTransaction.getNomeReceiver())
                            .append(", Date: ")
                            .append(lastTransaction.getDate());
                }
                int i=Server.getAccounts().size()-1;
                if(account==Server.getAccounts().get(i)) {
                    outputWriter.print(response.toString());
                    outputWriter.flush();
                }else{
                    outputWriter.println(response.toString());
                    outputWriter.flush();
                }
            }
        }

        return ""; // Empty string to avoid sending any extra data
    }

    public String handleQuit() throws IOException {
        Server.saveAccounts();
        outputWriter.println("Goodbye!");
        clientSocket.close();
        return "Goodbye!";
    }

    private Account findAccount(String accountName) {
        synchronized (Server.getAccounts()) {
            for (Account account : Server.getAccounts()) {
                if (account.getName().equals(accountName)) {
                    return account;
                }
            }
        }
        return null;
    }
}
