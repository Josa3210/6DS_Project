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
public class RestControllerAgent
{
    private final Client client;

    @Autowired
    public RestControllerAgent(Client client)
    {
        this.client = client;
    }

    @PostMapping("/agents/sync")
    private void Sync(@RequestBody Map<String, Object> request)
    {

    }
}
