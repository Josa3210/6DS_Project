package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.ClientFilesResponse;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@RestController
public class RestControllerDiscAndBoot {
    private static final String multicast_address = "224.0.1.1";
    private static Config config;
    private static HazelcastInstance hazelcastInstance;
    private final Client client;

    @Autowired
    public RestControllerDiscAndBoot(Client client) {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
        this.client = client;
    }

    /**
     * This REST request will set up the client
     *
     * @param request the IP, Port and nr of nodes of the naming server
     */
    @PostMapping("/welcome")
    public void welcome(@RequestBody Map<String, Object> request) {
        int nrNodes = Integer.parseInt(request.get("nrNodes").toString());
        String ipNamingServer = request.get("ip").toString();
        int portNamingServer = Integer.parseInt(request.get("port").toString());

        client.setupClient(nrNodes, ipNamingServer, portNamingServer);

        System.out.println(">> Welcome " + client.getHostname() + "!");
        System.out.println("* Nr nodes: " + nrNodes);
        System.out.println("* IP Naming Server: " + ipNamingServer);
        System.out.println("* Port Naming Server: " + portNamingServer);
    }

    @GetMapping("/multicastaddress")
    public String getMulticastAddress() {
        return multicast_address;
    }

    @GetMapping("/createinstance")
    public Object createHazelcastInstance() {
        return JSONObject.wrap(Hazelcast.newHazelcastInstance(config));
    }

    @PostMapping("/isReplicatedNode")
    public void isReplicatedNode(@RequestBody Map<String, Object> request) throws IOException {

        String filepath = (String) request.get("filepath");
        Inet4Address ip = (Inet4Address) InetAddress.getByName((String) request.get("original ip"));
        int id = (int) request.get("original id");
        client.receiveReplicatedFile(ip, id, filepath);
    }

    @PostMapping("/changeLoggerOriginal")
    public void changeLoggerOriginal(@RequestBody Map<String, Object> request) {
        String originalIP = (String) request.get("original ip");
        int originalID = (int) request.get("original id");
        int fileID = (int) request.get("file id");

        client.getLogger().putOriginal(fileID, originalID, originalIP);
    }

    @PostMapping("/deleteReplicatedFile")
    public void deleteReplicatedFile(@RequestBody Map<String, Object> request) throws UnknownHostException {

        String filename = (String) request.get("filename");
        String filepath = (String) request.get("filepath");

        try {
            client.isReplicatedFile = true;
            Files.delete(Path.of(filepath));
        } catch (IOException e) {
            System.out.println("-> File entry cannot be deleted successfully .");
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/OpenTCPConnection")
    public String OpenTCPConnection(@RequestBody Map<String, Object> request) throws IOException {
        String response;
        String ip = (String) request.get("replicated ip");
        try{
            client.clientSocket = new Socket(ip, 5000);
        } catch (ConnectException e){
            System.err.println(Arrays.toString(e.getStackTrace()));
        }
        response = "* Socket (" + ip + ", 5000) still connected";
        return response;
    }

    @PostMapping("/StartFileTransfer")
    public void StartFileTransfer(@RequestBody Map<String, Object> request) throws UnknownHostException {
        String filepath = (String) request.get("filepath");
        System.out.println(">> Starting file transfer: " + filepath);
        client.SendFile(filepath);
    }

    @GetMapping("/host")
    public String GetHostName()
    {
        return client.getHostname();
    }

    @GetMapping("/getFiles")
    public ClientFilesResponse GetClientFiles()
    {
        ClientFilesResponse response = new ClientFilesResponse();
        response.setClientFiles(client.getLogger().getDifferentFiles(client.getCurrentID()));
        return response;
    }
}
