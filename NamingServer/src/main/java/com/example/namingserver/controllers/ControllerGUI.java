package com.example.namingserver.controllers;

import com.example.namingserver.NamingServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.Inet4Address;
import java.util.HashMap;
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
        // Get all nodes
        Map<Integer, Inet4Address> data = namingServer.returnData();
        Map<Integer, NodeDetails> detailedData = new HashMap<>();

        for (Map.Entry<Integer, Inet4Address> entry : data.entrySet()) {
            int nodeId = entry.getKey();
            Inet4Address ipAddress = entry.getValue();
            int[] linkIds = namingServer.giveLinkIds(nodeId);
            detailedData.put(nodeId, new NodeDetails(nodeId, ipAddress, linkIds[0], linkIds[1]));
        }

        model.addAttribute("detailedData", detailedData);
        return "index";
    }

    public static class NodeDetails {
        private final int nodeId;
        private final Inet4Address ipAddress;
        private final int prevId;
        private final int nextId;

        public NodeDetails(int nodeId, Inet4Address ipAddress, int prevId, int nextId) {
            this.nodeId = nodeId;
            this.ipAddress = ipAddress;
            this.prevId = prevId;
            this.nextId = nextId;
        }

        public int getNodeId() {
            return nodeId;
        }

        public Inet4Address getIpAddress() {
            return ipAddress;
        }

        public int getPrevId() {
            return prevId;
        }

        public int getNextId() {
            return nextId;
        }
    }
}
