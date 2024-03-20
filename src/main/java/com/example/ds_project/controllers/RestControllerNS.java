package com.example.ds_project.controllers;

import com.example.ds_project.NamingServer;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
public class RestControllerNS {
    NamingServer namingServer = new NamingServer();


    /**
     * Asks the naming server for the location of a file
     *
     * @param fileName the name of the file
     * @return the IP-address of the file location
     */
    @GetMapping("/project/searchFile")
    public Inet4Address SearchFile(@RequestParam String fileName) {
        return namingServer.getLocationIP(fileName);
    }

    /**
     * Adds a node via the naming server with it's ip address
     *
     * @param request the JSON request body with "ip" as ip address
     */
    @PostMapping("/project/addNode")
    public void AddNode(@RequestBody Map<String, Object> request) throws UnknownHostException {
        Inet4Address ipAddress = (Inet4Address) InetAddress.getByName((String) request.get("ip"));
        String nodeName = (String) request.get("name");
        namingServer.addNodeIP(nodeName, ipAddress);
    }

    /**
     * Removes a node via the naming server with it's ip address
     *
     * @param request the JSON request body with "ip" as ip address
     */
    @PostMapping("/project/removeNode")
    public void RemoveNode(@RequestBody Map<String, Object> request) {
        Inet4Address ipAddress = (Inet4Address) request.get("ip");
        String nodeName = (String) request.get("name");
        namingServer.removeNodeIP(nodeName, ipAddress);
    }

    @GetMapping("/test")
    public String TestConnection(@RequestParam String testString)
    {
        return ("Test Communication : " + testString +"\n");
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
