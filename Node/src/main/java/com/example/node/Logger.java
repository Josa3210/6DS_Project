package com.example.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Logger {


    private final String fileName = "logger.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String filePath;
    private HashMap<Integer, Inet4Address> nodeMap;

    /**
     * Constructor to initialize the file path for the logger.
     */
    public Logger() {

        try {

            String currentPath = new java.io.File("").getCanonicalPath();
            String filepath = currentPath + "/Data/node/logger";
            Path path = Paths.get(filepath);
            System.out.println(filepath);
            Files.createDirectories(path);

            this.filePath = filepath;
            System.out.println("Logger will be saved in: " + filePath);

        } catch (IOException e) {

            System.out.println(e);
        }

    }

    /**
     * Loads the mapping from the JSON file into the logger.
     * If the file does not exist, initializes an empty map.
     */

    public void load() {

        try {
            File file = new File(filePath + "/" + fileName);

            if (file.exists()) {

                // Read the JSON file and convert it to HashMap
                HashMap<String, String> stringKeyMap = objectMapper.readValue(file, new TypeReference<HashMap<String, String>>() {
                });

                // Convert keys from String to Integer and values from String to Inet4Address
                nodeMap = new HashMap<>();

                for (Map.Entry<String, String> entry : stringKeyMap.entrySet()) {

                    try {

                        Integer key = Integer.parseInt(entry.getKey());
                        InetAddress inetAddress = InetAddress.getByName(entry.getValue());

                        if (inetAddress instanceof Inet4Address) {

                            nodeMap.put(key, (Inet4Address) inetAddress);

                        } else {

                            System.err.println("Error: " + entry.getValue() + " is not a valid IPv4 address.");
                        }
                    } catch (NumberFormatException | UnknownHostException e) {

                        System.err.println("Error parsing key-value pair: " + entry.getKey() + " - " + entry.getValue());
                    }
                }

                System.out.println("Map loaded from logger: " + filePath);

            } else {

                // Create a new file if it doesn't exist
                if (file.createNewFile()) {

                    System.out.println("File does not exist. Initializing an empty hashmap: " + fileName);
                    nodeMap = new HashMap<>();
                    // Save the empty hashmap
                    save();
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading hashmap from file: " + e.getMessage());
            nodeMap = new HashMap<>();
        }
    }


    /**
     * Saves the mapping from the logger to the JSON file.
     */
    public void save() {

        try {

            // Write the JSON string to the file
            File file = new File(filePath + "/" + fileName);
            objectMapper.writeValue(file, nodeMap);

            System.out.println("Map saved to file: " + filePath + "/" + fileName);

        } catch (IOException e) {

            System.err.println("Error saving map to file: " + e.getMessage());
        }
    }

    /**
     * Retrieves the IPv4 address corresponding to the given hash from the logger.
     *
     * @param hash the hashed integer key.
     * @return the IPv4 address associated with the given hash, or null if not found.
     */


    public Inet4Address get(Integer hash) {

        if (nodeMap != null) {
            Inet4Address address = nodeMap.get(hash);

            if (address == null) {
                System.err.println("Given key: hash. Result: null");
                return null;

            } else {
                return address;

            }
        }

        else {

            System.err.println("Hashmap is not initialized. Please load the hashmap first.");

            return null;
        }
    }


    /**
     * Retrieves the set of keys from the nodeMap.
     *
     * @return A Set of Integer keys from the nodeMap, or an empty Set if nodeMap is not initialized.
     */
    public Set<Integer> getKeys() {

        if (nodeMap != null) {

            return nodeMap.keySet(); // Assuming nodeMap is a Map<Integer, Something>
        }

        else {
            System.err.println("Hashmap is not initialized. Please load the map first.");
            return Collections.emptySet(); // Or return null if appropriate
        }
    }


    /**
     * Adds a new entry to the logger with the given hash and IPv4 address.
     *
     * @param hash the hashed integer key.
     * @param ip4  the IPv4 address to be associated with the hash.
     */
    public void put(Integer hash, Inet4Address ip4) {
        if (nodeMap != null) {
            nodeMap.put(hash, ip4);
            this.save();
        } else {
            System.err.println("Map is not initialized. Please load the map first.");
        }
    }

    /**
     * Removes an entry from the logger
     *
     */
    public void remove(int hash) {
        if (nodeMap != null) {

            if (nodeMap.containsKey(hash)) {
                nodeMap.remove(hash);
                System.out.println("Entry with key " + hash + " removed from the database.");
                this.save();

            } else {
                System.out.println("Entry has already been removed");
            }
        } else {
            System.err.println("Database is not initialized. Please load the database first.");
        }
    }

    public static void main(String[] args) {
        Logger logger = new Logger();
        logger.load();


    }

}





