package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RestControllerShutdown
{
    private final Client client;
    public String folderPath;

    @Autowired
    public RestControllerShutdown(Client client) {
        this.client = client;
    }

    @PostMapping("/shutdown/updateID")
    public void updateID() {
        int[] ids = client.requestLinkIds();
        client.setPrevID(ids[0]);
        client.setNextID(ids[1]);
    }
    @PostMapping("/shutdown/sendFiles")
    public void sendFiles(@RequestBody Map<String, Object> requestbody) throws IOException {
        String filepath;

        Logger logger = client.getLogger();
        Inet4Address newReplicatedIP;
        String originalIP_String = (String) requestbody.get("originalIP");
        Inet4Address originalIP = (Inet4Address) InetAddress.getByName(originalIP_String);

        HashMap<Integer, Inet4Address> nodeMap = (HashMap<Integer, Inet4Address>) requestbody.get("nodeMap");

        HashMap<Integer, String> fileMap = (HashMap<Integer, String>) requestbody.get("fileMap");

        for(Map.Entry<Integer, Inet4Address> entry : nodeMap.entrySet()){
            if(client.currentIP.equals(entry.getValue())){
                newReplicatedIP = client.requestIP(client.getPrevID());
                RestTemplate restTemplate = new RestTemplate();
                String url = "http://" + newReplicatedIP.getHostAddress()+":8080/isReplicatedNode";
                Map<String, Object> bodyNewReplicated = new HashMap<>();
                bodyNewReplicated.put("hashValue", entry.getKey());
                bodyNewReplicated.put("filepath", entry.getValue());
                bodyNewReplicated.put("original ip", originalIP);
                restTemplate.postForEntity(url, bodyNewReplicated, Void.class);
            }
            else{
                filepath = fileMap.get(entry.getKey());
                logger.load();
                logger.put(entry.getKey(), entry.getValue());
                logger.putFile(entry.getKey(), filepath);
                logger.save();
                client.sendReplicatedFile(originalIP, filepath);
            }

        }
    }

}

