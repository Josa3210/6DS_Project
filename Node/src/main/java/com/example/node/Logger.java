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
            System.out.println("\n!! Error: "+ e.getMessage());
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
                    fileArray = new JSONArray();
                    save();
                }
            }
        } catch (IOException e) {
            System.err.println("\n!! Error loading hashmap from file: " + e.getMessage());
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
            System.err.println("\n!! Error saving map: " + e.getMessage());
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
        }
    }

    /**
     * Adds the information of the owner (aka replicated node) of the file.
     *
     * @param id      of the file that is replicated
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
     *
     * @param id         of the file that is replicated
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
     *
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
        }
        return false;
    }

    /**
     * Removes the file with the given hash from the JSONArray.
     * This method is used when a file is deleted.
     *
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
        }
        return false;
    }

    /**
     * Returns the JSONArray containing all the files
     *
     * @return
     */
    public JSONArray getFileArray() {
        return fileArray;
    }

    public String printLogger() {
        StringBuilder str = new StringBuilder("""
                Logger
                ======================================
                """);
        for (int i = 0; i < fileArray.length(); i++) {
            JSONObject obj = fileArray.getJSONObject(i);
            String filename = (String) obj.get("filename");
            int id = (int) obj.get("hash");
            JSONObject owner = obj.getJSONObject("owner");
            String ownerIP = owner.getString("IP");
            int ownerID = (int) owner.getInt("ID");
            JSONObject original = obj.getJSONObject("original");
            String originalIP = original.getString("IP");
            int originalID = (int) original.getInt("ID");
            str.append("File: ").append(filename).append(", Hash: ").append(id).append("\n");
            str.append("- Owner: ").append(ownerID).append(" (/").append(ownerIP).append(")\n");
            str.append("- Original: ").append(originalID).append(" (/").append(originalIP).append(")\n");
            str.append("--------------------------------------\n");
        }
        return str.toString();
    }

    public int getOwnerID(JSONObject obj){
        JSONObject original = obj.getJSONObject("owner");
        return original.getInt("ID");
    }


    public int getOriginalID(JSONObject obj){
        JSONObject original = obj.getJSONObject("original");
        return original.getInt("ID");
    }

    /**
     * Checks for a certain file if the given node is the owner (aka replicated node)
     * @param fileID ID of the file to search for
     * @param requestID ID of the node to see if is the owner
     */
    public boolean isOwner(int fileID, int requestID) {
        if (fileArray != null) {
            for (int i = 0; i < fileArray.length(); i++) {
                JSONObject obj = fileArray.getJSONObject(i);
                int hash = obj.getInt("hash");
                if (Objects.equals(hash, fileID)) {
                    return getOwnerID(obj) == requestID;
                }
            }
        }
        return false;
    }


    /**
     * Checks for a certain file if the given node is the original (aka replicated node)
     * @param fileID ID of the file to search for
     * @param requestID ID of the node to see if is the original
     */
    public boolean isOriginal(int fileID, int requestID) {
        if (fileArray != null) {
            for (int i = 0; i < fileArray.length(); i++) {
                JSONObject obj = fileArray.getJSONObject(i);
                int hash = obj.getInt("hash");
                if (Objects.equals(hash, fileID)) {
                    return getOriginalID(obj) == requestID;
                }
            }
        }
        return false;
    }

    /**
     * Returns a list of lists containing the owned file and created files respectively
     * @param nodeID the id to check against
     */
    public ArrayList<ArrayList<String>> getDifferentFiles(int nodeID){
        ArrayList<ArrayList<String>> returnList = new ArrayList<>();
        ArrayList<String> ownedFiles = new ArrayList<>();
        ArrayList<String> createdFiles = new ArrayList<>();
        if (fileArray != null) {
            for (int i = 0; i < fileArray.length(); i++) {
                JSONObject obj = fileArray.getJSONObject(i);
                if (getOriginalID(obj) == nodeID) createdFiles.add(obj.getString("filename"));
                if (getOwnerID(obj) == nodeID) ownedFiles.add(obj.getString("filename"));
            }
        }
        returnList.add(ownedFiles);
        returnList.add(createdFiles);
        return returnList;
    }
}











