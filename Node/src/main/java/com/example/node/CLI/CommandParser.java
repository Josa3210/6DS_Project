package com.example.node.CLI;

import java.util.Arrays;

public class CommandParser {

    public CommandParser() {
    }

    public Command parse(String inputString) {
        String[] args = inputString.split(" ");
        String commandName = args[0];
        Command command = Command.NONE;
        switch (commandName) {

            case "ping":
                command = Command.PING;
                if (args.length - 1 > command.getNrArgs()) {
                    System.out.println("Too many arguments");
                }
                command.setArgs(Arrays.copyOfRange(args, 1, args.length));
            case "shutdown":

        }
        return command;
    }
}
