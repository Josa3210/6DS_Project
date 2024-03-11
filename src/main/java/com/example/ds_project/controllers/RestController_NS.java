package com.example.ds_project.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class RestController_NS
{
    /**
     * Asks the naming server for the location of a file
     * @param fileName the name of the file
     * @return the IP-address of the file location
     */
    @GetMapping("/project/searchFile")
    public Inet4Address SearchFile(@RequestParam String fileName) throws UnknownHostException
    {
        Inet4Address ip = (Inet4Address) InetAddress.getByName("192.168.1.60");

        // if file found
        if(false)
            return ip;
        // if file not found
        else
            return null;

    }
}
