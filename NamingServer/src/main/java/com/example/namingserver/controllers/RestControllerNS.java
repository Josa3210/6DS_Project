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

    @GetMapping("/ns/searchFile")
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
    public void removeNode(@RequestBody Map<String, Object> request)
    {
        String ipNode = (String) request.get("ip");
        namingServer.removeNodeIP(ipNode);
    }

    @GetMapping("/ns/getIp/{id}")
    public String getIp(@PathVariable("id") int id)
    {
        return namingServer.getIP(id).getHostAddress();
    }

    @GetMapping("/test")
    public String testConnection(@RequestParam String testString) {
        return ("Test Communication : " + testString + "\n");
    }

    @GetMapping("/ns/giveLinkID/{id}")
    public int[] GiveLinkID(@PathVariable("id") int id)
    {
        return namingServer.giveLinkIds(id);
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
