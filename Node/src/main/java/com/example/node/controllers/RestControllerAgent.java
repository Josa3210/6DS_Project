package com.example.node.controllers;

import com.example.node.Agents.FailureAgent;
import com.example.node.Client;
import com.example.node.Agents.SyncAgent;
import com.example.node.NodeFileEntry;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
public class RestControllerAgent
{
    private final Client client;

    @Autowired
    public RestControllerAgent(Client client)
    {
        this.client = client;
    }

    @GetMapping("/agents/sync")
    private SyncAgent Sync()
    {
        System.out.println(">> Answering Agent Sync request");
        return client.getSyncAgent();
    }

    @PostMapping("/agents/passFailureAgent")
    private void activatePassedAgent(@RequestBody Map<Object, Object> body) {
        // Get parameters
        int failedID = (int) body.get("failedID");
        int callingID = (int) body.get("callingID");

        // Get the agent
        FailureAgent agent = new FailureAgent(failedID, callingID, client.getNamingServerIP());

        // Run the agent and wait for thread to finish
        boolean passOn = agent.activateAgent(this.client.getCurrentID(), this.client.getLogger(), client.folderPath);

        // Get next id
        int[] ids = this.client.requestLinkIds();
        int nextID = ids[1];

        System.out.println("Pass on to next? " + passOn);
        if (passOn) {
            try {
                Inet4Address nextIP = (Inet4Address) InetAddress.getByName(client.requestIP(nextID));
                String newUrl = "http:/" + nextIP + ":8080/agents/passFailureAgent";

                System.out.println("Sending agent to " + newUrl);
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("agent", agent);

                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForEntity(newUrl, requestBody, Void.class);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
    }
}