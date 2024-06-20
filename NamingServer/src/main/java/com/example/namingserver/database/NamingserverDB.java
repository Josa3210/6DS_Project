package com.example.namingserver.database;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a naming server database responsible for managing the mapping
 * between hashed integers and IPv4 addresses.
 */
public class NamingserverDB implements I_NamingserverDB {


    private final String fileName = "hashmap.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String filePath;
    private HashMap<Integer, Inet4Address> nodeMap;

    /**
     * Constructor to initialize the file path for the database.
     */
    public NamingserverDB() {
        try {
            String currentPath = new java.io.File("").getCanonicalPath();
            String filepath = currentPath + "/Data/DB/namingServer";
            Path path = Paths.get(filepath);
            Files.createDirectories(path);

            this.filePath = filepath;

            System.out.println("\n>> Creating database in: " + filePath);

        } catch (IOException e) {
            System.out.println(e);
        }

    }

    /**
     * Loads the mapping from the JSON file into the database.
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
            } else {
                // Create a new file if it doesn't exist
                if (file.createNewFile()) {
                    System.out.println("* File does not exist. Initializing an empty hashmap: " + fileName);
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
     * Saves the mapping from the database to the JSON file.
     */
    public void save() {
        try {
            // Write the JSON string to the file
            File file = new File(filePath + "/" + fileName);
            objectMapper.writeValue(file, nodeMap);

            System.out.println("\n>> Map saved");

        } catch (IOException e) {
            System.err.println("\n>> Error saving map to file: " + e.getMessage());
        }
    }

    /**
     * Retrieves the IPv4 address corresponding to the given hash from the database.
     *
     * @param hash the hashed integer key.
     * @return the IPv4 address associated with the given hash, or null if not found.
     */


    public Inet4Address get(Integer hash) {
        if (nodeMap != null) {
            Inet4Address address = nodeMap.get(hash);
            if (address == null) {
                System.err.println("\n>> Given key:" + hash + " => not found");
                return null;
            } else {
                return address;
            }
        } else {
            System.err.println("\n>> Hashmap is not initialized. Please load the hashmap first.");
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
        } else {
            System.err.println("\n>> Hashmap is not initialized. Please load the map first.");
            return Collections.emptySet(); // Or return null if appropriate
        }
    }


    /**
     * Adds a new entry to the database with the given hash and IPv4 address.
     *
     * @param hash the hashed integer key.
     * @param ip4  the IPv4 address to be associated with the hash.
     */
    public void put(Integer hash, Inet4Address ip4) {
        if (nodeMap != null) {
            nodeMap.put(hash, ip4);
            this.save();
        } else {
            System.err.println("\n>> Map is not initialized. Please load the map first.");
        }
    }

    /**
     * Removes an entry from the database0
     */
    public void remove(int hash) {
        if (nodeMap != null) {
            if (nodeMap.containsKey(hash)) {
                nodeMap.remove(hash);
                System.out.println("\n>> Entry with key " + hash + " removed from the database.");
                this.save();
            } else {
                System.out.println("\n>> Entry has already been removed");
            }
        } else {
            System.err.println("\n>> Database is not initialized. Please load the database first.");
        }
    }

    @Override
    public void print() {
        System.out.println(this.nodeMap);
    }

    public HashMap<Integer, Inet4Address> getNodeMap() {
        return nodeMap;
    }
}




