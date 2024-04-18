package com.example.node.controllers;

import com.example.ds_project.Client.Client;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.util.Map;

@RestController
public class RestControllerShutdown
{
    private final Client client = new Client();

    @PostMapping("/shutdown/updateID")
    public void updateID(@RequestBody Map<String, Object> request)
    {
        int prevID = Integer.parseInt((String) request.get("prevID"));
        int nextID = Integer.parseInt((String) request.get("nextID"));

        client.receiveLinkID(prevID, nextID);
    }
}
