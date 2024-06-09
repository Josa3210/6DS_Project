package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.Logger;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.shaded.org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
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

        //else
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
        logger.put(fileHash, ip);
        logger.putFile(fileHash, filepath);
        logger.save();
        client.sendReplicatedFile(ip, filepath);
    }

    @PostMapping("/deleteReplicatedFile")
    public void deleteReplicatedFile(@RequestBody Map<String, Object> request) throws UnknownHostException {

        int fileHash = Integer.parseInt(request.get("hashValue").toString());
        String filename = (String) request.get("filename");
        String filepath = (String) request.get("filepath");
        System.out.println("filepath: " + filepath);
        System.out.println("path of filepath: " + Path.of(filepath));

        System.out.println("Delete file " + filename + " with hash: " + fileHash);

        try {
            Files.delete(Path.of(filepath));
            System.out.println("File entry deleted successfully");
        } catch (IOException e) {
            System.out.println("File entry cannot be deleted successfully .");
            throw new RuntimeException(e);
        }
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
        System.out.println("Starting file transfer: " + filepath);
        System.out.println(filepath);
        client.SendFile(filepath);
    }
}
