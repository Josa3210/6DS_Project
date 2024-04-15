package com.example.ds_project.Client.controllers;

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
    public void updateNextID(@RequestBody Map<String, Object> request)
    {
        int nodeID = Integer.parseInt((String) request.get("ID"));

        client.receiveLinkID();
    }
}
