package com.example.node.CLI;


import com.example.node.Client;
import com.example.node.I_Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

@Component
public class CLIDaemon {
    Scanner scanner;
    CommandParser commandParser;
    Client client;

    @Autowired
    public CLIDaemon(Client client) {
        this.scanner = new Scanner(System.in);
        this.commandParser = new CommandParser();
        this.client = client;
    }

    public void run() {
        String inputString = "";
        Command command;
        String[] args;
        while (true) {
            System.out.print("> ");
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
                case GETLINKIDS -> client.printLinkIds();
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
