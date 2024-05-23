package com.example.namingserver.controllers;

import com.example.namingserver.I_NamingServer;
import com.example.namingserver.NamingServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
public class RestControllerNS {
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

    @PostMapping("/ns/reportFileName")
    public void reportFileName(@RequestBody Map<String, Object> requestBody) throws UnknownHostException {

        // Check if the file name does not end with .swp --> temporary files!

        String filename = (String) requestBody.get("filename");


        if (!filename.endsWith(".swp")) {
            String ipAddressString = (String) requestBody.get("ip");
            String filepath = (String) requestBody.get("filepath");
            System.out.println("filepath reveiced: " + filepath);
            Inet4Address originalIP = (Inet4Address) InetAddress.getByName(ipAddressString);
            Integer operation = (Integer) requestBody.get("operation");
            Integer nextID = (Integer) requestBody.get("ID");
            namingServer.filePath = filepath;
            namingServer.reportLogger(filename, originalIP, operation,nextID);
        }
    }
}
