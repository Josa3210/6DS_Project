package com.example.node.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.shaded.org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestControllerDiscAndBoot {

    private static final String multicast_address = "224.0.1.1";
    private static Config config;

    private static HazelcastInstance hazelcastInstance;

    public RestControllerDiscAndBoot(){
        hazelcastInstance =  Hazelcast.newHazelcastInstance();

    }

    @GetMapping("/multicastaddress")
    public String getMulticastAddress(){
        return multicast_address;
    }

    @GetMapping("/createinstance")

    public Object createHazelcastInstance() throws JsonProcessingException {
        return JSONObject.wrap((Object) Hazelcast.newHazelcastInstance(config));
    }


}
