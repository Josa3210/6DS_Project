package com.example.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.pqc.jcajce.provider.Falcon;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Logger {


    private String fileName = "logger.json";
    private String filePath;
    private String tempPath;
    private FileWriter JSONWriter;
    private JSONArray nodeMap = new JSONArray();

    /**
     * Constructor to initialize the file path for the logger.
     */
    public Logger(String hostname) {
        try {
            // Create directories
            String directoryPath = new java.io.File("").getCanonicalPath()+"/Data/node";
            new File(directoryPath).mkdirs();

            // Create file
            this.fileName = "logger_" + hostname + ".json";
            this.filePath = directoryPath + "/" + this.fileName; // Append nodeName to differentiate logger directories;
            this.tempPath = directoryPath + "/" + "logger_temp.json";
            load();

        } catch (IOException e) {
            System.out.println("^^^^Error");
            System.out.println(e);
        }
    }

    public static void main(String[] args) {
        String hostname = "node1";
        Logger logger = new Logger(hostname);
        logger.load();
    }

    /**
     * Loads the mapping from the JSON file into the logger.
     * If the file does not exist, initializes an empty map.
     */

    public void load() {
        System.out.println("^^^^Trying to load from: " + filePath);
        try {
            File file = new File(filePath);
            if (file.exists()) {
                // Read the JSON file and convert it to HashMap
                String jsonContent = new String(Files.readAllBytes(Paths.get(this.filePath)));
                this.nodeMap = new JSONArray(jsonContent);

                System.out.println("^^^^Map loaded successfully");

            } else {
                // Create a new file if it doesn't exist
                if (file.createNewFile()) {
                    System.out.println("^^^^File does not exist. Initializing an empty file: " + fileName);
                    nodeMap = new JSONArray();

                    // Save the empty hashmap
                    save();
                }
            }
        } catch (IOException e) {
            System.err.println("^^^^Error loading hashmap from file: " + e.getMessage());
            nodeMap = new JSONArray();
        }
    }

    /**
     * Saves the mapping from the logger to the JSON file.
     */
    public void save() {
        try {
            File tempFile = new File(this.tempPath);
            File loggerFile = new File(this.filePath);

            // Create temp file
            tempFile.createNewFile();

            // Write in temp file
            this.JSONWriter = new FileWriter(this.tempPath);
            this.nodeMap.write(this.JSONWriter);
            this.JSONWriter.close();

            // Delete old file
            loggerFile.delete();

            // Rename temp file
            tempFile.renameTo(loggerFile);

            System.out.println("^^^^Map saved");
        } catch (IOException e) {
            System.err.println("^^^^Error saving map: " + e.getMessage());
        }
    }

    /**
     * Retrieves the IPv4 address corresponding to the given hash from the logger.
     *
     * @param hash the hashed integer key.
     * @return the IPv4 address associated with the given hash, or null if not found.
     */


    public JSONObject get(Integer hash) {
        if (nodeMap != null) {
            for (int i = 0; i < nodeMap.length(); i++) {
                JSONObject obj = nodeMap.getJSONObject(i);
                int id = (int) obj.get("hash");
                if (id == hash) return obj;
            }
            System.err.println("No value with " + hash + " found.");
        } else {
            System.err.println("Hashmap is not initialized. Please load the hashmap first.");
        }
        return null;
    }

    public JSONObject get(String filename) {
        if (nodeMap != null) {
            for (int i = 0; i < nodeMap.length(); i++) {
                JSONObject obj = nodeMap.getJSONObject(i);
                String objFilename = (String) obj.get("filename");
                if (Objects.equals(objFilename, filename)) return obj;
            }
            System.err.println("No value with " + filename + " found.");
        } else {
            System.err.println("Hashmap is not initialized. Please load the hashmap first.");
        }
        return null;
    }

    /**
     * Adds a new entry to the logger with the given hash and IPv4 address.
     *
     * @param hash the hashed integer key.
     */
    public void put(Integer hash, String fileName) {
        if (nodeMap != null) {
            JSONObject newObj = new JSONObject();
            newObj.put("hash", hash);
            newObj.put("filename", fileName);
            newObj.put("owner", new JSONObject());
            newObj.put("original", new JSONObject());
            nodeMap.put(newObj);
            save();
        } else {
            System.err.println("Map is not initialized. Please load the map first.");
        }
    }

    public void putOwner(int id, int ownerID, String ownerIP) {
        JSONObject obj = get(id);
        JSONObject ownerObj = new JSONObject().put("ID", ownerID).put("IP", ownerIP);
        obj.put("owner", ownerObj);
        save();
    }

    public void putOriginal(int id, int originalID, String originalIP) {
        JSONObject obj = get(id);
        JSONObject ownerObj = new JSONObject().put("ID", originalID).put("IP", originalIP);
        obj.put("original", ownerObj);
        save();
    }

    public void removeOwner(int id) {
        JSONObject obj = get(id);
        obj.remove("owner");
        save();
    }

    public void removeOriginal(int id) {
        JSONObject obj = get(id);
        obj.remove("original");
        save();
    }

    /**
     * Removes an entry from the logger
     */
    public boolean remove(int hash) {
        if (nodeMap != null) {
            for (int i = 0; i < nodeMap.length(); i++) {
                JSONObject obj = nodeMap.getJSONObject(i);
                int id = (int) obj.get("hash");
                if (id == hash) {
                    nodeMap.remove(i);
                    save();
                    return true;
                }
            }
            System.err.println("Node not in nodeMap");
        } else {
            System.err.println("Hashmap is not initialized. Please load the hashmap first.");
        }
        return false;
    }

    /**
     * Removes an entry from the logger
     */
    public boolean remove(String filename) {
        if (nodeMap != null) {
            for (int i = 0; i < nodeMap.length(); i++) {
                JSONObject obj = nodeMap.getJSONObject(i);
                String id = (String) obj.get("filename");
                if (Objects.equals(id, filename)) {
                    nodeMap.remove(i);
                    save();
                    return true;
                }
            }
            System.err.println("Node not in nodeMap");
        } else {
            System.err.println("Hashmap is not initialized. Please load the hashmap first.");
        }
        return false;
    }

    public JSONArray getNodeMap() {
        return nodeMap;
    }
}






