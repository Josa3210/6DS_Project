package com.example.ds_project.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Map;

/**
 * This class is responsible for saving a map to a JSON file.
 */
@Component
public class NamingserverDB {

    @Value("${map.file.path}")
    private final String filePath;
    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * Constructor to initialize the file path for the database.
     * @param filePath the path to the JSON file where the database will be saved.
     */
    public NamingserverDB(String filePath) {
        this.filePath = filePath;
        System.out.println("Namingserver database saved in: " + filePath);
    }

    /**
     * Saves the given map to a JSON file.
     *
     * @param nodeMap a map containing integer keys and Inet4Address values.
     *                The integers represent positive values limited to 32768,
     *                resulting from a hashing function, and the Inet4Address
     *                values represent IP addresses belonging to unique nodes
     *                in a ring topology.
     */
    public void saveMapToFile(Map<Integer, Inet4Address> nodeMap) {
        // Inet4Address represents an Internet Protocol version 4 (IPv4) address
        try {
            // Convert the map to JSON string
            String jsonMap = objectMapper.writeValueAsString(nodeMap);

            // Write the JSON string to the file
            File file = new File(filePath);
            objectMapper.writeValue(file, jsonMap);

            System.out.println("Map saved to file: " + filePath);
        } catch (IOException e) {
            System.err.println("Error saving map to file: " + e.getMessage());
        }
    }
    public void save(){


    }

    public void put(Integer hash, Inet4Address ip4){


    }


}




