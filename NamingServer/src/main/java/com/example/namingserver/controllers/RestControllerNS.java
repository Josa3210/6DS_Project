package com.example.namingserver.controllers;

import com.example.namingserver.I_NamingServer;
import com.example.namingserver.NamingServer;
import org.springframework.web.bind.annotation.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
public class RestControllerNS {

    I_NamingServer namingServer = new NamingServer();


    /**
     * Asks the naming server for the location of a file
     *
     * @param fileName the name of the file
     * @return the IP-address of the file location
     */

    @GetMapping("/project/searchFile")
    public Inet4Address searchFile(@RequestParam String fileName) {
        Inet4Address address = namingServer.getLocationIP(fileName);
        System.out.println(address);
        return address;
    }

    /**
     * Adds a node via the naming server with it's ip address
     *
     * @param request the JSON request body with "ip" as ip address
     */
    @PostMapping("/project/addNode")
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
    @PostMapping("/project/removeNode")
    public void removeNode(@RequestBody Map<String, Object> request) throws UnknownHostException {
        String failedNode = (String) request.get("failedNode");
        namingServer.removeNodeIP(failedNode);
    }

    @GetMapping("/project/getIp")
    public Inet4Address getIp(@RequestParam String failedID){
        int searchID = Integer.parseInt(failedID);
        return namingServer.getIP(searchID);
    }

    @GetMapping("/test")
    public String testConnection(@RequestParam String testString) {
        return ("Test Communication : " + testString + "\n");
    }

    @GetMapping("/ns/giveLinkID")
    public int[] GiveLinkID(@RequestBody Map<String, Object> request)
    {
        int nodeID = (Integer) request.get("nodeID");
        return namingServer.giveLinkIds(nodeID);
    }

    @GetMapping("/project/getIp")
    public Inet4Address GetNode(@RequestParam String id)
    {
        int nodeID = Integer.parseInt(id);
        return namingServer.getIp(nodeID);
    }

    /**
     * Sets the naming server for the rest controller
     *
     * @param namingServer the naming server
     */
    public void setNamingServer(NamingServer namingServer) {
        this.namingServer = namingServer;
    }
}
