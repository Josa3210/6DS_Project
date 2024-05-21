package com.example.namingserver.controllers;

import com.example.namingserver.NamingServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.Inet4Address;
import java.util.Map;

@Controller
public class ControllerGUI
{
    NamingServer namingServer;

    @Autowired
    public ControllerGUI(NamingServer namingServer)
    {
        this.namingServer = namingServer;
    }

    @GetMapping("/index")
    public String Dashboard(Model model)
    {
        // get all nodes
        Map<Integer, Inet4Address> data = namingServer.returnData();
        model.addAttribute("data", data);
        return "index";
    }
}
