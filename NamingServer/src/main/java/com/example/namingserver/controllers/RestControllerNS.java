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
        System.out.println("^^^^Returning IP of node " + id);
        String ip = namingServer.getIP(id).getHostAddress();
        System.out.println("* IP: " + ip);
        return ip;
    }

    @GetMapping("/test")
    public String testConnection(@RequestParam String testString) {
        return ("Test Communication : " + testString + "\n");
    }

    @GetMapping("/ns/giveLinkID/{id}")
    public int[] GiveLinkID(@PathVariable("id") int id) {
        return namingServer.giveLinkIds(id);
    }
    
    @GetMapping("/ns/getLocation/{filename}")
    public String[] getLocation(@PathVariable("filename") String filename) {
        String[] response = new String[2];
        // Check if the file name does not end with .swp --> temporary files!
        if (!filename.endsWith(".swp")) {
            int replicatedID = namingServer.getFileOwner(filename);
            Inet4Address replicatedIP = namingServer.getIP(replicatedID);
            
            response = new String[]{String.valueOf(replicatedID), replicatedIP.getHostAddress()};
            return response;
        }
        return response;
    }
}
