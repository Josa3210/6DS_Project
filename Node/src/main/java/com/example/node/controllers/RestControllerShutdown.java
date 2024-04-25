package com.example.node.controllers;

import com.example.node.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RestControllerShutdown
{
    private final Client client;

    @Autowired
    public RestControllerShutdown(Client client) {
        this.client = client;
    }

    @PostMapping("/shutdown/updateID")
    public void updateID(@RequestBody Map<String, Object> request)
    {
        int prevID = Integer.parseInt(request.get("prevID").toString());
        int nextID = Integer.parseInt(request.get("nextID").toString());
        System.out.println("updating REST received---------");
        client.receiveLinkID(prevID, nextID);
    }
}
