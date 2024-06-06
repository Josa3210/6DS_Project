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

        HashMap<String, String> stringNodeMap = (HashMap<String, String>) requestbody.get("nodeMap");

        HashMap<String, Inet4Address> nodeMap = new HashMap<>();
        for (Map.Entry<String, String> entry : stringNodeMap.entrySet()) {
            nodeMap.put(entry.getKey(), (Inet4Address) InetAddress.getByName(entry.getValue()));
        }

        HashMap<String, String> fileMap = (HashMap<String, String>) requestbody.get("fileMap");

        for(Map.Entry<String, Inet4Address> entry : nodeMap.entrySet()){
            Integer keyAsInteger = Integer.parseInt(entry.getKey());

            if(client.currentIP.equals(entry.getValue())){
                newReplicatedIP = client.requestIP(client.getPrevID());
                RestTemplate restTemplate = new RestTemplate();
                String url = "http://" + newReplicatedIP.getHostAddress()+":8080/ns/sendFiles";
                Map<String, Object> bodyNewReplicated = new HashMap<>();
                bodyNewReplicated.put("originalIP", originalIP);
                bodyNewReplicated.put("nodeMap", nodeMap);
                bodyNewReplicated.put("fileMap", fileMap);
                restTemplate.postForEntity(url, bodyNewReplicated, Void.class);
                break;

            }
            else{
                filepath = fileMap.get(entry.getKey());
                System.out.println("filepath: " + filepath);
                logger.load();
                logger.put(keyAsInteger, entry.getValue());
                logger.putFile(keyAsInteger, filepath);
                logger.save();
                client.sendReplicatedFile(originalIP, filepath);
            }

        }
    }

}

