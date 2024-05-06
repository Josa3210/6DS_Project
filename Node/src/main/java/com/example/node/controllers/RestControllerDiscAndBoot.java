package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.I_Client;
import com.example.node.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.shaded.org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
public class RestControllerDiscAndBoot {
    private static final String multicast_address = "224.0.1.1";
    private static Config config;
    private static HazelcastInstance hazelcastInstance;
    Client client;

    @Autowired
    public RestControllerDiscAndBoot(Client client) {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
        this.client = client;
    }

    @PostMapping("/welcome")
    public void welcome(@RequestBody Map<String, Object> request)
    {
        System.out.println("Welcome --------------");
        int nrNodes =  Integer.parseInt(request.get("nrNodes").toString());
        System.out.println("nrNodes: " + nrNodes);
        Inet4Address ipAddress = null;
        try
        {
            ipAddress = (Inet4Address) InetAddress.getByName((String) request.get("ip"));
            System.out.println("ipAddr: " + ipAddress);
        }
        catch (UnknownHostException e) {throw new RuntimeException(e);}
        int port = Integer.parseInt(request.get("port").toString());
        System.out.println("port: " + port);
        client.setupClient(nrNodes, ipAddress, port);

        // We start the filemonitorthread from here

        Thread filemonitorthread = client.fileMonitorThread;
        filemonitorthread.start();
    }

    @GetMapping("/multicastaddress")
    public String getMulticastAddress() {
        return multicast_address;
    }

    @GetMapping("/createinstance")
    public Object createHazelcastInstance() throws JsonProcessingException {
        return JSONObject.wrap((Object) Hazelcast.newHazelcastInstance(config));
    }

    @PostMapping("/isReplicatedNode")
    public void isReplicatedNode(@RequestBody Map<String, Object> request) throws UnknownHostException {

        Integer fileHash = Integer.parseInt(request.get("hashValue").toString());
        Inet4Address ip = (Inet4Address) InetAddress.getByName((String) request.get("original ip"));
        Logger logger = client.getLogger();
        System.out.println("This is the replicated node for file: " + fileHash);
        logger.load();

        // Puts the filehash and the ip address of the node where the file was created in the logger ..
        logger.put(fileHash, ip);
    }


}
