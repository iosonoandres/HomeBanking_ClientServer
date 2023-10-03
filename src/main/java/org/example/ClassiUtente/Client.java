package org.example.ClassiUtente;

import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java Client <server-ip> <server-port>");
            return;
        }


        String serverIp = args[0];


        try {

            int serverPort = Integer.parseInt(args[1]);

            try{



            try (Socket socket = new Socket(serverIp, serverPort);
                 BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                 PrintWriter outputWriter = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                System.out.println("Connected to the server.");
                System.out.println("Type <help> for a list of commands");
                System.out.print(">");

                //inizio di ascolto thread

                Thread messageListenerThread = new Thread(() -> {
                    try {
                        while (true) {
                            String responseLine;
                            while ((responseLine = responseReader.readLine()) != null) {
                                if (responseLine.equals("END_RESPONSE")) {
                                    System.out.print(">");
                                    break; // Stop reading when the delimiter is encountered
                                }
                                System.out.println(responseLine);
                            }
                        }
                    } catch (IOException e) {
                        // An IOException will occur when the socket is closed
                        System.out.println("Server has been disconnected.");
                    }
                });
                messageListenerThread.start();

// while tasked to read user input
                try {
                    while (true) {
                        String command = inputReader.readLine();
                        outputWriter.println(command);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading input.");
                }

// Wait for the messageListenerThread to finish
                try {
                    messageListenerThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        } catch (ConnectException ce) {
            System.err.println("Connection to the server was refused.");
        } catch (IOException e) {
            System.err.println("An error occurred during communication: " + e.getMessage());
        }
        } catch (NumberFormatException nfe) {
            System.err.println("Invalid server port: " + args[1] + ". Please provide a valid integer.");
        }

    }
}

