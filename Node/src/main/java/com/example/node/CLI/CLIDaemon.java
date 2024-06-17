package com.example.node.CLI;


import com.example.node.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
                client.shutDown();
                return;
            }


            command = commandParser.parse(inputString);
            args = command.getArgs();
            switch (command) {
                case PING -> client.ping(Integer.parseInt(args[0]));
                case SHUTDOWN -> client.shutDown();
                case GETLINKIDS -> client.printLinkIds();
                case GETNAME -> client.getName();
            }
        }
    }

    public String getInput() {
        return this.scanner.nextLine();
    }
}
