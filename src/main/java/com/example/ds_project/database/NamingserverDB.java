package com.example.ds_project.database;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

/**
 * This class represents a naming server database responsible for managing the mapping
 * between hashed integers and IPv4 addresses.
 */
public class NamingserverDB implements I_NamingserverDB {


    private String filePath;
    private final String fileName = "hashmap.txt";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private HashMap<Integer, Inet4Address> nodeMap; // Inet4Address = key,

    /**
     * Constructor to initialize the file path for the database.
     *
     */
    public NamingserverDB()
    {
        try
        {
            String currentPath = new java.io.File(".").getCanonicalPath();
            String filepath = currentPath + "/Data/DB/namingServer";
            Path path = Paths.get(filepath);

            System.out.println(filepath);

            Files.createDirectories(path);

            this.filePath = filepath;

            System.out.println("Database will be saved in: " + filePath);

        }
        catch(IOException e)
        {
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

            if (file.exists()) // if there exists a file for the give filepath ..
            {
                // Read the JSON file and convert it to HashMap
                nodeMap = objectMapper.readValue(file, HashMap.class);
                System.out.println("Map loaded from file: " + filePath);
            }
            else {

                System.out.println("File does not exist. Initializing an empty map.");
                nodeMap = new HashMap<>();
            }

        } catch (IOException e) {

            System.err.println("Error loading map from file: " + e.getMessage());
            nodeMap = new HashMap<>();
        }
    }

    /**
     * Saves the mapping from the database to the JSON file.
     */
    public void save() {

        try {
            // Convert the HashMap (nodeMap) to a JSON string
            String jsonMap = objectMapper.writeValueAsString(nodeMap);

            // Write the JSON string to the file
            File file = new File(filePath + "/" + fileName);
            objectMapper.writeValue(file, jsonMap);

            System.out.println("Map saved to file: "  + filePath + "/" + fileName);

        } catch (IOException e) {
            System.err.println("Error saving map to file: " + e.getMessage());
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
            return nodeMap.get(hash);

        } else {
            System.err.println("Hashmap is not initialized. Please load the hashmap first.");

            return null;
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

        } else {

            System.err.println("Map is not initialized. Please load the map first.");
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
            System.err.println("Map is not initialized. Please load the map first.");
            return Collections.emptySet(); // Or return null if appropriate
        }
    }

    /**
     * Removes an entry from the database0
     */
    public void remove(int hash) {
        // TODO Check if node is in nodeMAP!
        if (nodeMap != null) {
            nodeMap.remove(hash);
            System.out.println("Entry with key " + hash + " removed from the database.");
        } else {
            System.err.println("Database is not initialized. Please load the database first.");
        }
    }

}






