package com.example.ds_project.database;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class NamingserverDB_test {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(NamingserverDB_test.class, args);

        // Create a sample map with node data
        Map<Integer, Inet4Address> nodeMap = new HashMap<>();
        try {

            Inet4Address node1 = (Inet4Address) Inet4Address.getByName("192.168.0.1");
            Inet4Address node2 = (Inet4Address) Inet4Address.getByName("192.168.0.2");

            // Adding nodes to the map
            nodeMap.put(12345, node1);
            nodeMap.put(54321, node2);

            // Retrieve the namingserverDB bean from the Spring context
            NamingserverDB namingserverDb = context.getBean(NamingserverDB.class);

            // Save the map to a JSON file
            namingserverDb.save();
        }

        catch (UnknownHostException e) {

            System.err.println("Error creating Inet4Address: " + e.getMessage());
        }
    }
}
