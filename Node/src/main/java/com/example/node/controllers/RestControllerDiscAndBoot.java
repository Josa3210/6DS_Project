package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.I_Client;
import com.example.node.Logger;
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

        if (nrNodes > 1) {
            Thread filemonitorthread = client.getFileMonitorThread();
            filemonitorthread.start();
        }

        else
            client.startFileMonitor = true;
    }

    @GetMapping("/multicastaddress")
    public String getMulticastAddress() { return multicast_address; }

    @GetMapping("/createinstance")
    public Object createHazelcastInstance() { return JSONObject.wrap(Hazelcast.newHazelcastInstance(config)); }

    @PostMapping("/isReplicatedNode")
    public void isReplicatedNode(@RequestBody Map<String, Object> request) throws IOException {

        Integer fileHash = Integer.parseInt(request.get("hashValue").toString());
        String filepath = (String) request.get("filepath");
        Inet4Address ip = (Inet4Address) InetAddress.getByName((String) request.get("original ip"));
        Logger logger = client.getLogger();
        System.out.println("This is the replicated node for file: " + fileHash);
        logger.load();
        // Puts the filehash and the ip address of the node where the file was created in the logger ..
        logger.putOwner(fileHash,client.getCurrentID(), client.getCurrentIP());
        client.serverSocket = new ServerSocket(5000);
        String url = "http://"+ip.getHostAddress()+":8080/OpenTCPConnection";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("replicated ip", InetAddress.getLocalHost().getHostAddress());
        String completed = String.valueOf(restTemplate.postForEntity(url, requestBody, String.class));
        System.out.println(completed);
        client.clientSocket = client.serverSocket.accept();
        DataInputStream dataInputStream = new DataInputStream(client.clientSocket.getInputStream());
        url = "http://" + ip.getHostAddress()+ ":8080/StartFileTransfer";
        requestBody.clear();
        requestBody.put("filepath", filepath);
        restTemplate.postForEntity(url, requestBody, Void.class);
        client.ReceiveFile(filepath,dataInputStream);
    }

    @PostMapping("/newNodeOwner")
    public void newNodeOwner(@RequestBody Map<String, Object> request){

        Integer fileHash = Integer.parseInt(request.get("hashValue").toString());
        System.out.println("This node becomes the new owner of file with hash: " + fileHash);

        // We change the ip from the original IP --> current IP
        Logger logger = client.getLogger();
        logger.load();

        // Change the originalOwner to this new client
        logger.putOriginal(fileHash,client.getCurrentID(), client.getCurrentIP());
    }

    @PostMapping("/OpenTCPConnection")
    public String OpenTCPConnection(@RequestBody Map<String, Object> request) throws IOException {
        String ip = (String) request.get("replicated ip");
        client.clientSocket = new Socket(ip,5000);
        return("TCP connection is established, ready for file transfer");
    }

    @PostMapping("/StartFileTransfer")
    public void StartFileTransfer(@RequestBody Map<String, Object> request) throws UnknownHostException {
        System.out.println("start file transfer");
        String filepath = (String) request.get("filepath");
        System.out.println(filepath);
        client.SendFile(filepath);
    }
}
