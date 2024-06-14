package com.example.node.Agents;

import com.example.node.Client;
import com.example.node.NodeFileEntry;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;

import java.util.ArrayList;
import java.util.List;

public class SyncAgent implements Runnable
{
    private List<NodeFileEntry> agentFiles;
    private final Client client;
    private final Agent syncAgent;
    private boolean isActive = false;

    public SyncAgent(Client client)
    {
        System.out.println("^^^^Debugging created syncAgent");
        this.client = client;
        this.syncAgent = new Agent();
        this.agentFiles = new ArrayList<>();
    }

    @Override
    public void run()
    {
        CyclicBehaviour behaviour = new SyncBehaviour(client, this);
        syncAgent.addBehaviour(behaviour);
        isActive = true;
        System.out.println("^^^^Debugging Run()");
        syncAgent.run();
    }

    public List<NodeFileEntry> getAgentFiles() { return agentFiles; }

    public void setAgentFiles(List<NodeFileEntry> agentFiles) {
        this.agentFiles = agentFiles;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
