package com.example.node;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Logger {


    private String fileName = "logger.json";
    private String filePath;
    private String tempPath;
    private JSONArray fileArray = new JSONArray();

    /**
     * Constructor to initialize the file path for the logger.
     */
    public Logger(String hostname) {
        try {
            // Create directories
            String directoryPath = new java.io.File("").getCanonicalPath() + "/Data/node";
            new File(directoryPath).mkdirs();

            // Create files
            this.fileName = "logger_" + hostname + ".json";
            this.filePath = directoryPath + "/" + this.fileName; // Append nodeName to differentiate logger directories;
            this.tempPath = directoryPath + "/" + "logger_temp.json";
            load();

        } catch (IOException e) {
            System.out.println("^^^^Error");
            System.err.println(e);
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
            // Define a file from where the file should be
            File file = new File(filePath);

            // If there is a file, read it and convert to a JSONArray
            if (file.exists()) {
                String jsonContent = new String(Files.readAllBytes(Paths.get(this.filePath)));
                this.fileArray = new JSONArray(jsonContent);

                // If there is no file, create one.
            } else {
                // Create a new file if it doesn't exist
                if (file.createNewFile()) {
                    System.out.println("^^^^File does not exist. Initializing an empty file: " + fileName);
                    fileArray = new JSONArray();
                    save();
                }
            }
        } catch (IOException e) {
            System.err.println("^^^^Error loading hashmap from file: " + e.getMessage());
            fileArray = new JSONArray();
        }
    }

    /**
     * Saves the mapping from the logger to the JSON file. This is done in 4 steps:
     * 1) Create a temporary file
     * 2) Write to that temporary file
     * 3) Remove the original file
     * 4) Rename the temporary file to the original file
     * <p>
     * This is because the writer holds a buffer and when calling .flush() or .close(), the content of this buffer gets written in the file.
     * The existing information is not overwritten.
     */
    public void save() {
        try {
            // Define the two files
            File tempFile = new File(this.tempPath);
            File loggerFile = new File(this.filePath);

            // Create temp file
            tempFile.createNewFile();

            // Write in temp file
            FileWriter JSONWriter = new FileWriter(this.tempPath);
            this.fileArray.write(JSONWriter);
            JSONWriter.close();

            // Delete old file
            loggerFile.delete();

            // Rename temp file
            tempFile.renameTo(loggerFile);
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
        if (fileArray != null) {
            // Go through all the indexes and compare against the given key.
            for (int i = 0; i < fileArray.length(); i++) {
                JSONObject obj = fileArray.getJSONObject(i);
                int id = (int) obj.get("hash");
                if (id == hash) return obj;
            }
        } else {
            System.err.println("Hashmap is not initialized. Please load the hashmap first.");
        }
        return null;
    }

    /**
     * Retrieves the IPv4 address corresponding to the given hash from the logger.
     *
     * @param filename the filename.
     * @return the IPv4 address associated with the given hash, or null if not found.
     */
    public JSONObject get(String filename) {
        if (fileArray != null) {
            // Go through all the indexes and compare against the given key.
            for (int i = 0; i < fileArray.length(); i++) {
                JSONObject obj = fileArray.getJSONObject(i);
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
     * Adds a new file to the logger with the given hash and IPv4 address.
     * The owner and original are still left blank. These can be set using the .putOwner() and .putOriginal() methods
     *
     * @param hash the hashed integer key.
     */
    public void put(Integer hash, String fileName) {
        if (fileArray != null && this.get(fileName) == null) {
            JSONObject newObj = new JSONObject();
            newObj.put("hash", hash);
            newObj.put("filename", fileName);
            newObj.put("owner", new JSONObject());
            newObj.put("original", new JSONObject());
            fileArray.put(newObj);
            save();
        } else {
            System.err.println("Map is not initialized. Please load the map first.");
        }
    }

    /**
     * Adds the information of the owner (aka replicated node) of the file.
     * @param id of the file that is replicated
     * @param ownerID ID of the owner
     * @param ownerIP IP of the owner
     */
    public void putOwner(int id, int ownerID, String ownerIP) {
        JSONObject obj = get(id);
        JSONObject ownerObj = new JSONObject().put("ID", ownerID).put("IP", ownerIP);
        obj.put("owner", ownerObj);
        save();
    }

    /**
     * Adds the information of the original creator of the file.
     * @param id of the file that is replicated
     * @param originalID ID of the original creator
     * @param originalIP IP of the original creator
     */
    public void putOriginal(int id, int originalID, String originalIP) {
        JSONObject obj = get(id);
        JSONObject ownerObj = new JSONObject().put("ID", originalID).put("IP", originalIP);
        obj.put("original", ownerObj);
        save();
    }

    /**
     * Removes the file with the given hash from the JSONArray.
     * This method is used when a file is deleted.
     * @param hash of the file to be deleted
     * @return if the deletion has happened or not
     */
    public boolean remove(int hash) {
        if (fileArray != null) {
            for (int i = 0; i < fileArray.length(); i++) {
                JSONObject obj = fileArray.getJSONObject(i);
                int id = (int) obj.get("hash");
                if (id == hash) {
                    fileArray.remove(i);
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
     * Removes the file with the given hash from the JSONArray.
     * This method is used when a file is deleted.
     * @param filename of the file to be deleted
     * @return if the deletion has happened or not
     */
    public boolean remove(String filename) {
        if (fileArray != null) {
            for (int i = 0; i < fileArray.length(); i++) {
                JSONObject obj = fileArray.getJSONObject(i);
                String id = (String) obj.get("filename");
                if (Objects.equals(id, filename)) {
                    fileArray.remove(i);
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
     * Returns the JSONArray containing all the files
     * @return
     */
    public JSONArray getFileArray() {
        return fileArray;
    }
}






