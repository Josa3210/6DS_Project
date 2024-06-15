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
        Logger logger = client.getLogger();
        JSONArray nodeMap = (JSONArray) requestbody.get("nodeMap");

        // For every file in the logger

        for (int i = 0; i < nodeMap.length(); i++) {
            JSONObject obj = nodeMap.getJSONObject(i);
            JSONObject owner = (JSONObject) obj.get("owner");
            String filename = obj.get("filename").toString();

            // Check if this node is the replicated node of the file
            String newReplicatedIP;
            if (client.getCurrentIP().equals(owner.get("IP"))) {
                // If yes, the file should be send to the prevID of this node
                newReplicatedIP = client.requestIP(client.getPrevID());

                // Send file from this node to the new replicated node (prevID)
                RestTemplate restTemplate = new RestTemplate();
                String url = "http://" + newReplicatedIP + ":8080/isReplicatedNode";
                Map<String, Object> bodyNewReplicated = new HashMap<>();
                bodyNewReplicated.put("originalIP", client.getCurrentIP());
                bodyNewReplicated.put("filepath", client.folderPath+"/"+filename);
                restTemplate.postForEntity(url, bodyNewReplicated, Void.class);

                // Update the logger on the fact that new owner the prevID is.
                logger.putOwner((int) obj.get("hash"), client.getPrevID(), newReplicatedIP);

                // Update the logger that this node is the new original
                logger.putOriginal((int) obj.get("hash"), client.getCurrentID(), client.getCurrentIP());

            } else {
                // If the node is not the replicated, it will become the new original
                logger.put((int) obj.get("hash"),(String) obj.get("filename"));

                // Update the logger that this node is the new original
                logger.putOriginal((int) obj.get("hash"), client.getCurrentID(), client.getCurrentIP());
            }
        }
    }
}

