package com.example.ds_project;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

import java.net.Inet4Address;
import java.util.Map;

@Component
public class mapsaver {

    @Value("${map.file.path}") // Path to the JSON file (configured in application.properties)

    private String filePath;

    private final ObjectMapper objectMapper = new ObjectMapper();


    // Save the map to a JSON file


    // integer -->

    public void saveMapToFile(Map<Integer, Inet4Address> nodeMap) {

        try {
            // Convert the map to JSON string
            String jsonMap = objectMapper.writeValueAsString(nodeMap);

            // Write the JSON string to the file
            File file = new File(filePath);
            objectMapper.writeValue(file, jsonMap);

            System.out.println("Map saved to file: " + filePath);

        }
        catch (IOException e) {
            System.err.println("Error saving map to file: " + e.getMessage());
        }
    }
}
