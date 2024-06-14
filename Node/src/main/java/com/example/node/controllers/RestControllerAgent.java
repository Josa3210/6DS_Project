package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.Agents.SyncAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
