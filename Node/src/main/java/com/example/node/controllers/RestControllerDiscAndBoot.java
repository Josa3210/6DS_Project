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
import java.util.concurrent.TimeUnit;

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

        /*if (nrNodes > 1) {
            // the number of clients > 1
            System.out.println("Number of nodes > 1, there are replicated nodes ..");
            client.startFileMonitor = true;
            System.out.println("number of nodes is bigger then 1");

        }

        else {
            System.out.println("Number of nodes < 1, no replicated nodes ..");
            client.startFileMonitor = false;
        }*/
    }

    @GetMapping("/multicastaddress")
    public String getMulticastAddress() { return multicast_address; }

    @GetMapping("/createinstance")
    public Object createHazelcastInstance() { return JSONObject.wrap(Hazelcast.newHazelcastInstance(config)); }

    @PostMapping("/isReplicatedNode")
    public void isReplicatedNode(@RequestBody Map<String, Object> request) throws IOException {

       String filepath = (String) request.get("filepath");
        Inet4Address ip = (Inet4Address) InetAddress.getByName((String) request.get("original ip"));
        client.sendReplicatedFile(ip, filepath);
    }

    @PostMapping("/deleteReplicatedFile")
    public void deleteReplicatedFile(@RequestBody Map<String, Object> request) throws UnknownHostException {

        String filename = (String) request.get("filename");
        String filepath = (String) request.get("filepath");
        System.out.println("filepath: " + filepath);

        try {
            client.isReplicatedFile = true;
            Files.delete(Path.of(filepath));
            client.getLogger().remove(filename);
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
