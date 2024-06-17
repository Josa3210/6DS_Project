package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.Logger;
import com.hazelcast.shaded.org.json.JSONArray;
import com.hazelcast.shaded.org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RestControllerShutdown {
    private final Client client;
    public String folderPath;

    @Autowired
    public RestControllerShutdown(Client client) {
        this.client = client;
    }

    /**
     * This REST request will update the client's previous and next ID's
     */
    @PostMapping("/shutdown/updateID")
    public void updateID() {
        int[] ids = client.requestLinkIds();
        client.setPrevID(ids[0]);
        client.setNextID(ids[1]);
    }

    @PostMapping("/shutdown/sendFiles")
    public void sendFiles(@RequestBody Map<String, Object> requestbody) throws IOException {
        // Get logger of this file
        Logger logger = client.getLogger();

        // Get all the files present on the leaving node
        String strArray = (String) requestbody.get("files");
        JSONArray files = new JSONArray(strArray);
        String originalIP = (String) requestbody.get("original ip");

        System.out.println("^^^^Sending files to the previous ID");
        // For every file in the logger
        for (int i = 0; i < files.length(); i++) {
            JSONObject obj = files.getJSONObject(i);
            JSONObject owner = (JSONObject) obj.get("owner");
            String filename = obj.get("filename").toString();
            // Check if this node is the replicated node of the file
            String newReplicatedIP;
            String ownerIP = (String) owner.get("IP");
            int fileID = (int) obj.get("hash");

            System.out.println("* File: " + filename);
            System.out.println("* " + client.getCurrentID() + " is owner of file?");
            if (client.getCurrentIP().equals(ownerIP)) {
                // If yes, the file should be sent to the prevID of this node
                newReplicatedIP = client.requestIP(client.getPrevID());

                System.out.println("* => YES: file sent to previous node: " + client.getPrevID());

                // Send file from this node to the new replicated node (prevID)
                RestTemplate restTemplate = new RestTemplate();
                String url = "http://" + newReplicatedIP + ":8080/isReplicatedNode";
                Map<String, Object> bodyNewReplicated = new HashMap<>();
                bodyNewReplicated.put("original ip", client.getCurrentIP());
                bodyNewReplicated.put("original id", client.getCurrentID());
                bodyNewReplicated.put("filepath", client.folderPath+"/"+filename);
                restTemplate.postForEntity(url, bodyNewReplicated, Void.class);

                // Update the logger on the fact that new owner the prevID is.
                logger.putOwner(fileID, client.getPrevID(), newReplicatedIP);

                // Update the logger that this node is the new original
                logger.putOriginal(fileID, client.getCurrentID(), client.getCurrentIP());

            } else {
                System.out.println("* => NO: notify owner that there is a new original");
                // Download the file
                String filepath = client.folderPath + "/" + filename;
                client.sendFile((Inet4Address) InetAddress.getByName(originalIP),filepath);

                // If the node is not the replicated, it will become the new original
                logger.put(fileID,(String) obj.get("filename"));

                // Set the owner of this file to the owner from the other logger
                logger.putOwner(fileID, (int) owner.get("ID"), ownerIP);

                // Update the logger that this node is the new original
                logger.putOriginal(fileID, client.getCurrentID(), client.getCurrentIP());

                // Notify the replicated node that this node is the new original
                RestTemplate restTemplate = new RestTemplate();
                String url = "http://" + ownerIP + ":8080/changeLoggerOriginal";
                Map<String, Object> bodyNewReplicated = new HashMap<>();
                bodyNewReplicated.put("original ip", client.getCurrentIP());
                bodyNewReplicated.put("original id", client.getCurrentID());
                bodyNewReplicated.put("file id", fileID);
                restTemplate.postForEntity(url, bodyNewReplicated, Void.class);
            }
        }
    }
}

