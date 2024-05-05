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
    public void updateID()
    {
        int[] ids = client.requestLinkIds();
        client.setPrevID(ids[0]);
        client.setNextID(ids[1]);
    }
}

