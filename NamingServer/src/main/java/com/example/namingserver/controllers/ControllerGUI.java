package com.example.namingserver.controllers;

import com.example.namingserver.NamingServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
public class ControllerGUI
{
    private final NamingServer namingServer;
    private final ExecutorService executorService;

    @Autowired
    public ControllerGUI(NamingServer namingServer)
    {
        this.namingServer = namingServer;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @GetMapping("/index")
    public String dashboard(Model model)
    {
        // Get all nodes
        Map<Integer, Inet4Address> data = namingServer.returnData();
        Map<Integer, NodeDetails> detailedData = new HashMap<>();

        for (Map.Entry<Integer, Inet4Address> entry : data.entrySet())
        {
            int nodeId = entry.getKey();
            Inet4Address ipAddress = entry.getValue();
            int[] linkIds = namingServer.giveLinkIds(nodeId);
            String hostName = namingServer.getHostNameClient(nodeId);
            detailedData.put(nodeId, new NodeDetails(nodeId, ipAddress, linkIds[0], linkIds[1], hostName));
        }

        model.addAttribute("detailedData", detailedData);
        return "index";
    }

    @PostMapping("/shutdown")
    public String shutdownNode(@RequestParam("nodeId") int nodeId)
    {
        executorService.submit(() ->  namingServer.shutdownClient(nodeId)); // immediate response
        return "redirect:/index"; // Redirect back to index page
    }

    public static class NodeDetails
    {
        private final int nodeId, prevId, nextId;
        private final Inet4Address ipAddress;
        private final String hostName;

        public NodeDetails(int nodeId, Inet4Address ipAddress, int prevId, int nextId, String hostName)
        {
            this.nodeId = nodeId;
            this.ipAddress = ipAddress;
            this.prevId = prevId;
            this.nextId = nextId;
            this.hostName = hostName;
        }

        public int getNodeId() {return nodeId;}
        public Inet4Address getIpAddress() {return ipAddress;}
        public int getPrevId() {return prevId;}
        public int getNextId() {return nextId;}
        public String getHostName() {return hostName;}
    }
}
