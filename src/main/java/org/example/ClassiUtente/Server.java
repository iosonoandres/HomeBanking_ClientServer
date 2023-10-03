package org.example.ClassiUtente;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    private static final int DEFAULT_PORT = 9000;

    private static List<Account> accounts;
    //a list of Client Handler objects
    private static ArrayList<ClientHandler> activeCHList;


    public static void main(String[] args) throws FileNotFoundException {

        activeCHList = new ArrayList<>();
        int port = (args.length > 0) ? Integer.parseInt(args[0]) : DEFAULT_PORT;

        // Load accounts from file
        accounts = DataManager.loadAccounts();
        if (accounts.isEmpty()) {
            accounts = new ArrayList<>();
            System.out.println("Server launched succesfully without accounts");
        }

        // thread that reads in command line waiting for "quit" command to terminate Server
        Thread terminalThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if (input.equals("quit")) {
                    // Esegui le operazioni per spegnere i client e il server
                    Iterator<ClientHandler> iterator = activeCHList.iterator();
                    while (iterator.hasNext()) {
                        ClientHandler activeCH = iterator.next();
                        if (activeCH.isAlive()) {
                            System.out.println("terminating Client associated to " + activeCH);
                            iterator.remove(); // Safely remove using iterator
                        } else {
                            // This Client Handler is already dead, and so is its Client
                            iterator.remove(); // Safely remove using iterator
                        }
                    }
                    System.out.println("Server terminated");
                    System.exit(0);
                } else {
                    System.out.println("Invalid command. Use 'quit' to terminate Server");
                }
            }
        });


        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.print("Server started and listening on port " + port+"\n");
            System.out.print("type 'quit' to terminate Server"+"\n");
            terminalThread.start();


            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
                activeCHList.add(clientHandler);
            }
        } catch (SocketException e) {
            System.out.println("SocketException");
        } catch (IOException e) {
            System.out.println("IOException");
        }

    }

    public static synchronized List<Account> getAccounts() {
        return accounts;
    }

    public static synchronized void saveAccounts() throws IOException {
        DataManager.saveAccounts(accounts);
    }
}
