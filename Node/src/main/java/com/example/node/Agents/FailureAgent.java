package com.example.node.Agents;

import com.example.node.Logger;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.abs;
import static java.util.Collections.max;

public class FailureAgent implements Runnable{

    private final int failedID;
    private final int callingID;
    private final String namingServerIP;
    private final Agent failureAgent;
    private String fileDirectory;
    private Logger logger;

    // Construct agent

    public FailureAgent(int failedID, int callingID, String namingServerIP) {
        this.failedID = failedID;
        this.callingID = callingID;
        this.namingServerIP = namingServerIP;
        this.fileDirectory = null;
        this.failureAgent = new Agent();
    }

    public boolean activateAgent(int currentID, Logger logger, String fileDirectory) {
        this.logger = logger;
        this.fileDirectory = fileDirectory;
        Thread agentThread = new Thread(this);
        agentThread.start();
        return currentID != callingID;
    }

    @Override
    public void run() {
        System.out.println(">> Trying to start behaviour");
        FailureAgentBehaviour behaviour = new FailureAgentBehaviour( this.failedID, this.logger, this.namingServerIP, this.fileDirectory);
        this.failureAgent.addBehaviour(behaviour);
        this.failureAgent.doActivate();
    }

    public static class FailureAgentBehaviour extends OneShotBehaviour {
        private final JSONArray fileList;
        private final int failedID;
        private final String namingServerIP;
        private final Logger logger;
        private final String fileDirectory;


        public FailureAgentBehaviour(int failedID, Logger logger, String namingServerIP, String fileDirectory) {
            this.fileList = logger.getFileArray();
            this.failedID = failedID;
            this.namingServerIP = namingServerIP;
            this.logger = logger;
            this.fileDirectory = fileDirectory;
        }

        @Override
        public void action() {
            RestTemplate restTemplate = new RestTemplate();
            System.out.println("=> FailureAgent: starting action");
            // Check for all files if the failed node is the owner
            for (int i = 0; i < fileList.length(); i++) {
                // Get filename and file owner
                JSONObject obj = fileList.getJSONObject(i);
                String filename = (String) obj.get("filename");
                int ownerID = getOwnerID(filename);

                System.out.println("- Owner of file " + filename + " : " + ownerID);
                if (ownerID == this.failedID) {
                    // Extract information from logger
                    int fileHash = obj.getInt("hash");
                    JSONObject original = obj.getJSONObject("original");
                    String originalIP = original.getString("IP");
                    int originalID = original.getInt("ID");

                    // Search new owner
                    String[] replicatedLocation = requestNewOwner(filename);
                    String replicationIP = replicatedLocation[1];
                    int replicatedID = Integer.parseInt(replicatedLocation[0]);

                    // Send that it is the new replicated node
                    String postUrl = "http://" + replicationIP + ":8080/isReplicatedNode";
                    HttpHeaders headers = new HttpHeaders();
                    Map<String, Object> requestBody = new HashMap<>();
                    HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

                    try {
                        // Send the POST request
                        requestBody.put("original ip", originalIP);
                        requestBody.put("original id", originalID);
                        requestBody.put("filepath", this.fileDirectory + "/" + filename);

                        System.out.println("- Sending request to: " + postUrl);
                        ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestEntity, Void.class);
                        HttpStatusCode statusCode = responseEntity.getStatusCode();

                        System.out.println("- Putting new owner in logger");
                        logger.putOwner(fileHash, replicatedID, replicationIP);

                        if (statusCode == HttpStatus.OK) {
                            System.out.println("!!Successfully replicated file (" + filename + ") to " + replicationIP);

                        } else {
                            System.err.println("!!Sending node list failed with status code: " + statusCode);
                        }
                    } catch (NumberFormatException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }

        private int getOwnerID(String fileName) {
            JSONObject obj = this.logger.get(fileName);
            JSONObject owner = (JSONObject) obj.get("owner");
            return (int) owner.get("ID");
        }

        private String[] requestNewOwner(String filename) {
            // Get ip of the new replicated node
            String getURL = "http://" + namingServerIP + ":8080/ns/getLocation/" + filename;

            // Make an HTTP GET request to report the hash value
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String[]> responseEntity = restTemplate.getForEntity(getURL, String[].class);

            return new String[]{responseEntity.getBody()[0], responseEntity.getBody()[1]};
        }
    }
}




