package com.example.ds_project.controllers;

import com.example.ds_project.NamingServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.Inet4Address;

@RestController
public class RestController_NS
{
    /**
     * Asks the naming server for the location of a file
     * @param fileName the name of the file
     * @return the IP-address of the file location
     */
    @GetMapping("/project/searchFile")
    public Inet4Address SearchFile(@RequestParam String fileName)
    {
        NamingServer ns = new NamingServer(); // TEMP
        return ns.getLocationIP(fileName);
    }
}
