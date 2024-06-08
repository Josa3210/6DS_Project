package com.example.node;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Scanner;

@Configuration
public class AppConfig {

    @Bean
    public Client client()
    {
        Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        System.out.println(">> Enter username: ");

        String userName = myObj.nextLine();  // Read user input
        return new Client(userName);
    }
}
