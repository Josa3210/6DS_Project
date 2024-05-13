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
    private final Client client;

    @Autowired
    public RestControllerDiscAndBoot(Client client)
    {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
        this.client = client;
    }

    /**
     * This REST request will set up the client
     * @param request the IP, Port and nr of nodes of the naming server
     */
    @PostMapping("/welcome")
    public void welcome(@RequestBody Map<String, Object> request)
    {
        int nrNodes =  Integer.parseInt(request.get("nrNodes").toString());
        String ipNamingServer = request.get("ip").toString();
        int portNamingServer = Integer.parseInt(request.get("port").toString());

        client.setupClient(nrNodes, ipNamingServer, portNamingServer);

        System.out.println(">> Welcome " + client.getHostname() + "!");
        System.out.println("* Nr nodes: " + nrNodes);
        System.out.println("* IP Naming Server: " + ipNamingServer);
        System.out.println("* Port Naming Server: " + portNamingServer);

        // We start the filemonitorthread from here
        Thread filemonitorthread = client.getFileMonitorThread();
        filemonitorthread.start();
    }

    @GetMapping("/multicastaddress")
    public String getMulticastAddress() { return multicast_address; }

    @GetMapping("/createinstance")
    public Object createHazelcastInstance() { return JSONObject.wrap(Hazelcast.newHazelcastInstance(config)); }

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

    @PostMapping("/newNodeOwner")
    public void newNodeOwner(@RequestBody Map<String, Object> request) throws UnknownHostException {

        Integer fileHash = Integer.parseInt(request.get("hashValue").toString());
        System.out.println("This node becomes the new owner of file with hash: " + fileHash);

        // We change the ip from the original IP --> current IP
        Logger logger = client.getLogger();
        logger.load();

        // We first remove the current entry with the original IP
        logger.remove(fileHash);

        // We add the current IP nex to the filehash
        Inet4Address clientIP = (Inet4Address) InetAddress.getByName(client.getCurrentIP());
        logger.put(fileHash, clientIP);

    }

}
