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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.HashMap;
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
        client.numberNodes = nrNodes;
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

        System.out.println("hello");
        // We start the filemonitorthread from here
        if (nrNodes > 1) {
            client.startFileMonitor = true;
        }
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
    public void isReplicatedNode(@RequestBody Map<String, Object> request) throws IOException {

        Integer fileHash = Integer.parseInt(request.get("hashValue").toString());
        String filepath = (String) request.get("filepath");
        Inet4Address ip = (Inet4Address) InetAddress.getByName((String) request.get("original ip"));
        Logger logger = client.getLogger();
        System.out.println("This is the replicated node for file: " + fileHash);
        logger.load();
        // Puts the filehash and the ip address of the node where the file was created in the logger ..
        logger.put(fileHash, ip);
        client.serverSocket = new ServerSocket(5000);
        String url = "http://"+ip+":8080/OpenTCPConnection";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("replicated ip", InetAddress.getLocalHost().getHostAddress());
        String completed = String.valueOf(restTemplate.postForEntity(url, requestBody, String.class));
        System.out.println(completed);
        client.clientSocket = client.serverSocket.accept();
        DataInputStream dataInputStream = new DataInputStream(client.clientSocket.getInputStream());
        url = "http://"+ip+":8080/StartFileTransfer";
        requestBody.clear();
        requestBody.put("filepath", filepath);
        restTemplate.postForEntity(url, requestBody, Void.class);
        client.ReceiveFile(filepath,dataInputStream);
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
        logger.put(fileHash, client.currentIP);

    }

    @PostMapping("/OpenTCPConnection")
    public String OpenTCPConnection(@RequestBody Map<String, Object> request) throws IOException {
        String ip = (String) request.get("replicated ip");
        client.clientSocket = new Socket(ip,5000);
        return("TCP connection is established, ready for file transfer");
    }

    @PostMapping("/StartFileTransfer")
    public void StartFileTransfer(@RequestBody Map<String, Object> request) throws UnknownHostException {
        String filepath = (String) request.get("filepath");
        client.SendFile(filepath);
    }
}
