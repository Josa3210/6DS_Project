package com.example.node;

import jade.core.behaviours.CyclicBehaviour;

import java.io.File;

public class SyncBehaviour extends CyclicBehaviour
{
    private Client client;
    private SyncAgent agent;

    public SyncBehaviour(Client client, SyncAgent agent)
    {
        this.client = client;
        this.agent = agent;
    }

    @Override
    public void action()
    {
        // Listing all files owned by the node at which this agent runs,
        System.out.println(">> All owned files of the client:");
        for(NodeFileEntry e : client.getFileList())
        {
            System.out.println("* File Name: " + e.getFilename() + " - Locked: " + e.isLocked());
        }
        // If one of the owned files is not added to the list, the list needs to be updated
        for(NodeFileEntry e : client.getFileList())
        {
            for(File file : agent.getAgentFiles())
            {
                if(!e.getFilename().equals(file.getName()))
                {
                    File newFile = new File(e.getFilename()); // ?? correct ??
                    agent.getAgentFiles().add(newFile);
                }
            }
        }
        // Update the list stored by the node based on the agent’s list
            // How to check if the file is from the node it's on?
        // If there is a lock request on the current node, and the file is not locked on the agent’s list, locking should be
        // enabled on the node and the list should be synchronized accordingly
            // lock request?
        // Remove the lock when it is not needed anymore, and update local file list accordingly
            // How?
    }
}
