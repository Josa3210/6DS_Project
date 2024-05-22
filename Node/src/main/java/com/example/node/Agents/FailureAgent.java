package com.example.node.Agents;

import com.example.node.NodeFileEntry;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.Inet4Address;
import java.util.List;
import java.util.Set;

import static java.lang.Math.abs;
import static java.util.Collections.max;

public class FailureAgent implements Runnable {

    private int failedID, callingID, namingServerPort;
    private String namingServerIP;
    private Agent failureAgent;
    private List<NodeFileEntry> fileList;

    // Construct agent

    public FailureAgent(int failedID, int callingID, int namingServerPort, String namingServerIP) {
        this.failedID = failedID;
        this.callingID = callingID;
        this.namingServerPort = namingServerPort;
        this.namingServerIP = namingServerIP;
        this.failureAgent = new Agent();
        this.fileList = null;
    }

    public boolean activateAgent(int currentID, List<NodeFileEntry> fileList) {
        this.fileList = fileList;
        this.run();

        return currentID != callingID;
    }

    @Override
    public void run() {
        FailureAgentBehaviour behaviour = new FailureAgentBehaviour(this.fileList, this.failedID);
        this.failureAgent.addBehaviour(behaviour);
    }

    public class FailureAgentBehaviour extends OneShotBehaviour {
        private List<NodeFileEntry> fileList;
        private int failedID;


        public FailureAgentBehaviour(List<NodeFileEntry> fileList, int failedID) {
            this.fileList = fileList;
            this.failedID = failedID;
        }

        @Override
        public void action() {
            // Check for all files if the failed node is the owner
            for (NodeFileEntry nodeFileEntry : this.fileList) {
                String[] ownerInfo = getOwnerID(nodeFileEntry.getFilename());

                int ownerID = Integer.parseInt(ownerInfo[0]);
                if (ownerID == this.failedID){
                    // If yes:
                    // Search new owner
                }
                // If new owner does not have file, transfer file to new owner
                // Change log
            }
        }

        private String[] getOwnerID(String fileName){
            String getUrl = "http://" + namingServerIP + ":" + namingServerPort + "/ns/searchFileOwnerID/?=" + fileName;
            RestTemplate restTemplate = new RestTemplate();

            System.out.println(">> Sending search file owner to NS)");
            System.out.println("* Get URL: " + getUrl);

            ResponseEntity<String[]> response = restTemplate.getForEntity(getUrl, String[].class);
            return response.getBody();
        }
    }
}



