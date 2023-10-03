package org.example.ClassiUtente;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    //private static final String FILE_PATH = "accounts.txt";

    public static List<Account> loadAccounts() throws FileNotFoundException {
        try
        {
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();
            Gson gson = builder.create();
            FileReader fileread=new FileReader("Account_List.json");
            BufferedReader bufferedReader = new BufferedReader(fileread);
            Type mapType = new TypeToken<List<Account>>(){}.getType();
            List<Account> lista= gson.fromJson(bufferedReader, mapType);
            return lista;
        }catch(FileNotFoundException e)
        {
            System.out.println("No files found, creating an empty arraylist");
            List<Account> lista=new ArrayList<>();
            return lista;
        }
    }


    public static void saveAccounts(List<Account> listaScrittura) throws IOException { //RICORDARSI DI METTERE TRY CATCH
        try
        {
            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting();

            Gson gson = builder.create();
            FileWriter writer = new FileWriter("Account_List.json");

            String ilToJson = gson.toJson(listaScrittura);
            writer.write(ilToJson);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}