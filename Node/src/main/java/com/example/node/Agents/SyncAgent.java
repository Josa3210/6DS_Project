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

    public SyncAgent(Client client)
    {
        this.client = client;
        this.syncAgent = new Agent();
        this.agentFiles = new ArrayList<>();
    }

    @Override
    public void run()
    {
        syncAgent.addBehaviour(new SyncBehaviour(client, this));
    }

    public List<NodeFileEntry> getAgentFiles() { return agentFiles; }

    public void setAgentFiles(List<NodeFileEntry> agentFiles) {
        this.agentFiles = agentFiles;
    }
}
