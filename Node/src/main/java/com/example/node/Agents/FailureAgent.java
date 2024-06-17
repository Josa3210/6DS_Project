package com.example.node.Agents;

import com.example.node.FileMonitor;
import com.example.node.Logger;
import com.example.node.NodeFileEntry;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.Inet4Address;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.abs;
import static java.util.Collections.max;

public class FailureAgent implements Runnable {

    private final int failedID;
    private final int callingID;
    private final int namingServerPort;
    private final String namingServerIP;
    private final Agent failureAgent;
    private List<NodeFileEntry> fileList;
    private Logger logger;

    // Construct agent

    public FailureAgent(int failedID, int callingID, int namingServerPort, String namingServerIP) {
        this.failedID = failedID;
        this.callingID = callingID;
        this.namingServerPort = namingServerPort;
        this.namingServerIP = namingServerIP;
        this.failureAgent = new Agent();
        this.fileList = null;
    }

    public boolean activateAgent(int currentID, List<NodeFileEntry> fileList, Logger logger) {
        this.fileList = fileList;
        this.logger = logger;
        this.run();
        return currentID != callingID;
    }

    @Override
    public void run() {
        FailureAgentBehaviour behaviour = new FailureAgentBehaviour(this.fileList, this.failedID, this.logger, this.namingServerIP, this.namingServerPort);
        this.failureAgent.addBehaviour(behaviour);
    }

    public class FailureAgentBehaviour extends OneShotBehaviour {
        private List<NodeFileEntry> fileList;
        private int failedID, namingServerPort;
        private String namingServerIP;
        private Logger logger;
        private String fileDirectory = "Data/node/Files";


        public FailureAgentBehaviour(List<NodeFileEntry> fileList, int failedID, Logger logger, String namingServerIP, int namingServerPort) {
            this.fileList = fileList;
            this.failedID = failedID;
            this.namingServerIP = namingServerIP;
            this.namingServerPort = namingServerPort;
            this.logger = logger;
        }

        @Override
        public void action() {
            // Check for all files if the failed node is the owner
            for (NodeFileEntry nodeFileEntry : this.fileList) {
                String filename = nodeFileEntry.getFilename();

                int ownerID = getOwnerID(filename);
                System.out.println("Owner of file " + filename + " is " + ownerID);
                if (ownerID == this.failedID) {
                    // Search new owner
                    requestNewOwner(filename);
                    // If new owner does not have file, transfer file to new owner
                    String filePath = fileDirectory + "/" + filename;
                    File file = new File(filePath);
                    if (!file.exists()) {
                        System.out.println("File (" + filePath + ") does not exist. Start transferring.");
                        // Transfer file
                    }
                }
            }
        }

        private int getOwnerID(String fileName) {
            JSONObject obj = this.logger.get(fileName);
            JSONObject owner = (JSONObject) obj.get("owner");
            return (int) owner.get("ID");
        }

        private void requestNewOwner(String filename) {
            int operation = 2;

            // Prepare the URL for reporting the hash value to the naming server
            String postUrl = "http://" + namingServerIP + ":8080/ns/reportFileName";

            Map<String, Object> requestBody = new HashMap<>();

            System.out.println("operation: " + operation);

            requestBody.put("filename", filename);
            requestBody.put("operation", operation);

            // Make an HTTP POST request to report the hash value
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestBody, Void.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                System.out.println("Hash value: " + filename + " correctly handled by server");
            } else {
                System.err.println("Failed to report hash value to naming server for file: " + filename);
            }
        }
    }
}




