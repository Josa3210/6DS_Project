package com.example.node.controllers;

import com.example.node.Client;
import com.example.node.I_Client;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.shaded.org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
public class RestControllerDiscAndBoot {
    private static final String multicast_address = "224.0.1.1";
    private static Config config;
    private static HazelcastInstance hazelcastInstance;
    Client client;

    @Autowired
    public RestControllerDiscAndBoot(Client client) {
        hazelcastInstance = Hazelcast.newHazelcastInstance();
        this.client = client;
    }

    @PostMapping("/welcome")
    public void welcome(@RequestBody Map<String, Object> request) {
        int nrNodes =  Integer.parseInt((String) request.get("nrNodes"));
        Inet4Address ipAddress = null;
        try {
            ipAddress = (Inet4Address) InetAddress.getByName((String) request.get("ip"));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        int port = Integer.parseInt((String) request.get("port"));

        client.setupClient(nrNodes, ipAddress, port);
    }

    @GetMapping("/multicastaddress")
    public String getMulticastAddress() {
        return multicast_address;
    }

    @GetMapping("/createinstance")

    public Object createHazelcastInstance() throws JsonProcessingException {
        return JSONObject.wrap((Object) Hazelcast.newHazelcastInstance(config));
    }


}
