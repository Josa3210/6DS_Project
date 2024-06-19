package com.example.node.Agents;

import com.example.node.NodeFileEntry;

import java.util.List;

public class SyncAgentResponse
{
    private List<NodeFileEntry> agentFiles;

    public List<NodeFileEntry> getAgentFiles() {
        return agentFiles;
    }

    public void setAgentFiles(List<NodeFileEntry> agentFiles) {
        this.agentFiles = agentFiles;
    }
}
