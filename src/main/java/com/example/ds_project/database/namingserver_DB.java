package com.example.ds_project.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.Map;

/**
 * This class is responsible for saving a map to a JSON file.
 */
@Component
public class namingserver_DB {

    @Value("${map.file.path}") // Path to the JSON file (configured in application.properties)
    private String filePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
}



