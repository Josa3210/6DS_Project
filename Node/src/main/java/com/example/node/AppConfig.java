package com.example.node;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Scanner;

@Configuration
public class AppConfig {

    @Bean
    public Client client()
    {
        // Create a Scanner object
        Scanner myObj = new Scanner(System.in);
        System.out.println(">> Enter username: ");

        // Read user input
        String userName = myObj.nextLine();
        return new Client(userName);
    }
}
