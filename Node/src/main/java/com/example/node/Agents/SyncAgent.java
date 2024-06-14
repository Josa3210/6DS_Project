package com.example.node.Agents;

import com.example.node.Client;
import com.example.node.NodeFileEntry;
import jade.core.Agent;
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
        SyncAgent sync = this;
        this.syncAgent = new Agent()
        {
            @Override
            protected void setup()
            {
                System.out.println("^^^^Debugging Added Behaviour");
                addBehaviour(new SyncBehaviour(client, sync));
            }
        };
        this.agentFiles = new ArrayList<>();
    }

    @Override
    public void run()
    {
        isActive = true;
        System.out.println("^^^^Debugging Run()");
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
