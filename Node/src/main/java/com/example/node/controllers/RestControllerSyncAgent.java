package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.SyncAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
public class RestControllerSyncAgent
{
    private final Client client;

    @Autowired
    public RestControllerSyncAgent(Client client)
    {
        this.client = client;
    }

    @PostMapping("/agents/sync")
    private void Sync(@RequestBody Map<String, Object> request)
    {
        // receive agent as parameter = agent
        SyncAgent agent = (SyncAgent) request.get("agent"); // works??

        // creates a thread from a received agent
            // Thread?
            // agent.getSyncAgent().
        // Start thread
            // xxx

        // Wait till thread is finished
            // xxx

        // execute REST on the next node
            // Get next node ip
        String ipNextNode = client.requestIP(client.getNextID());
            // Send Post
        String postUrl = "http://" + ipNextNode + ":8080/agents/sync";
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestBody = new HashMap<>()
        {{
            put("agent", client.getSyncAgent());
        }};
        restTemplate.postForEntity(postUrl, requestBody, Void.class);
    }
}
