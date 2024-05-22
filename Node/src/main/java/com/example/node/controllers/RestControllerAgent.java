package com.example.node.controllers;

import com.example.node.Agents.FailureAgent;
import com.example.node.Client;
import com.example.node.NodeFileEntry;
import com.hazelcast.core.Hazelcast;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class RestControllerAgent {

    private final Client client;

    @Autowired
    public RestControllerAgent(Client client) {
        this.client = client;
    }

    @PostMapping("/agent/passFailureAgent")
    private void activatePassedAgent(@RequestBody Map<Object, Object> body) {
        // Get the agent
        FailureAgent agent = (FailureAgent) body.get("agent");

        // Get next id
        int[] ids = this.client.requestLinkIds();
        int nextID = ids[1];
        List<NodeFileEntry> fileList = this.client.getFileList();

        // Run the agent and wait for thread to finish
        boolean passOn = agent.activateAgent(this.client.getCurrentID(), fileList);

        if (passOn) {
            try {
                Inet4Address nextIP = (Inet4Address) InetAddress.getByName(client.requestIP(nextID));
                String postUrl = "http://" + nextIP + "/agent/passFailureAgent";

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("agent", agent);

                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForEntity(postUrl, requestBody, Void.class);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
