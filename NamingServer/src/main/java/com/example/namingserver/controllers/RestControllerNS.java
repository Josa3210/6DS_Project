package com.example.namingserver.controllers;

import com.example.namingserver.I_NamingServer;
import com.example.namingserver.NamingServer;
import com.hazelcast.shaded.org.json.JSONArray;
import org.springframework.web.bind.annotation.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RestControllerNS
{
    NamingServer namingServer = new NamingServer();

    /**
     * Adds a node via the naming server with it's ip address
     *
     * @param request the JSON request body with "ip" as ip address
     */
    @PostMapping("/ns/addNode")
    public void addNode(@RequestBody Map<String, Object> request) throws UnknownHostException {
        String ipAddressString = (String) request.get("ip");
        Inet4Address ipAddress = (Inet4Address) InetAddress.getByName(ipAddressString);
        String nodeName = (String) request.get("name");
        namingServer.addNodeIP(nodeName, ipAddress);
    }

    /**
     * Removes a node via the naming server with it's ip address
     *
     * @param request the JSON request body with "ip" as ip address
     */
    @PostMapping("/ns/removeNode")
    public void removeNode(@RequestBody Map<String, Object> request) {
        int nodeID = Integer.parseInt(request.get("nodeID").toString());
        namingServer.removeNodeIP(nodeID);
    }

    @GetMapping("/ns/getIp/{id}")
    public String getIp(@PathVariable("id") int id) {
        return namingServer.getIP(id).getHostAddress();
    }

    @GetMapping("/test")
    public String testConnection(@RequestParam String testString) {
        return ("Test Communication : " + testString + "\n");
    }

    @GetMapping("/ns/giveLinkID/{id}")
    public int[] GiveLinkID(@PathVariable("id") int id) {
        return namingServer.giveLinkIds(id);
    }


    /**
     * Reports the hash of a newly created file on the node and calculates if there are replicated nodes
     *
     * @param requestBody the JSON request body with "ip" as ip address
     */

    @PostMapping("/ns/deleteReplicatedFile")
    public void deleteReplicatedFile(@RequestBody Map<String, Object> requestBody) throws UnknownHostException {

        // Check if the file name does not end with .swp --> temporary files!
        String filename = (String) requestBody.get("filename");
        if (!filename.endsWith(".swp")) {
            String filepath = (String) requestBody.get("filepath");
            String ipAddressString = (String) requestBody.get("ip");
            Inet4Address originalIP = (Inet4Address) InetAddress.getByName(ipAddressString);
            Integer nextID = (Integer) requestBody.get("ID");

            System.out.println("filepath received: " + filepath);

            namingServer.deleteReplicatedFile(filename, filepath, originalIP, nextID);
        }
    }

    @PostMapping("/ns/createReplicatedFile")
    public void createReplicatedFile(@RequestBody Map<String, Object> requestBody) throws UnknownHostException {

        // Check if the file name does not end with .swp --> temporary files!
        String filename = (String) requestBody.get("filename");
        if (!filename.endsWith(".swp")) {
            String filepath = (String) requestBody.get("filepath");
            String ipAddressString = (String) requestBody.get("ip");
            Inet4Address originalIP = (Inet4Address) InetAddress.getByName(ipAddressString);
            Integer nextID = (Integer) requestBody.get("ID");

            System.out.println("^^^^Received file: " + filepath);
            namingServer.replicate(filename, filepath, originalIP, nextID);
        }
    }

    @PostMapping("ns/shutdown")
    public void shutdown(@RequestBody Map<String, Object> request) throws UnknownHostException{
        int PrevID = (Integer) request.get("PrevID");
        JSONArray nodeMap = (JSONArray) request.get("nodeMap");
        String originalIPString = (String) request.get("originalIP");
        Inet4Address originalIP = (Inet4Address) InetAddress.getByName(originalIPString);
        namingServer.shutdown_node(PrevID, nodeMap, originalIP);
    }
}
