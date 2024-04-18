package com.example.node.CLI;



import com.example.node.I_Client;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class CLIDaemon {
    Scanner scanner;
    CommandParser commandParser;
    I_Client client;

    public CLIDaemon(I_Client client) {
        this.scanner = new Scanner(System.in);
        this.commandParser = new CommandParser();
        this.client = client;
    }

    public void run() {
        String inputString = "";
        Command command;
        String[] args;
        while (true) {
            print(">");
            inputString = getInput();
            if (inputString.equals("exit")) {
                return;
            }


            command = commandParser.parse(inputString);
            args = command.getArgs();
            switch (command) {
                case PING -> {
                    try {
                        client.ping((Inet4Address) InetAddress.getByName(args[0]), args[1]);
                    } catch (UnknownHostException e) {
                        print("Could not find address: " + args[0]);
                    }
                }
                case SHUTDOWN -> client.shutDown();
            }
        }
    }

    public void print(String string) {
        System.out.println(string);
    }

    public String getInput() {
        return this.scanner.nextLine();
    }
}
