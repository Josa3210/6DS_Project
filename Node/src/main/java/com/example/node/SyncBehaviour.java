package com.example.node;

import jade.core.behaviours.CyclicBehaviour;

import java.io.File;
import java.util.List;

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
        System.out.println("---------------------------------------------");
        // If one of the owned files is not added to the list, the list needs to be updated
        for(NodeFileEntry e : client.getFileList())
        {
            // Check if client files are in the list on the agent, otherwise add it to the agent list
            boolean fileFound = false;
            for(NodeFileEntry file : agent.getAgentFiles())
            {
                if(e.getFilename().equals(file.getFilename()))
                {
                    fileFound = true;
                    break;
                }
            }

            if(!fileFound)
            {
                System.out.println("* File not found: " + e.getFilename() + " - Adding to Agent List");
                NodeFileEntry newFile = new File(e.getFilename()); // ?? correct ??
                agent.getAgentFiles().add(newFile);
            }
        }
        // Update the list stored by the node based on the agent’s list
        for(File file : agent.getAgentFiles())
        {
            // Check if all agent files are in the list on the client, otherwise add it to the client list
            boolean fileFound = false;
            for(NodeFileEntry e : client.getFileList())
            {
                if(file.getName().equals(e.getFilename()))
                {
                    fileFound = true;
                    break;
                }
            }

            if(!fileFound)
            {
                System.out.println("* File not found: " + file.getName() + " - Adding to Client List");
                NodeFileEntry entry = new NodeFileEntry(file.getName());
                client.getFileList().add(entry);
            }
        }
        // If there is a lock request on the current node, and the file is not locked on the agent’s list, locking should be
        // enabled on the node and the list should be synchronized accordingly
            // lock request?
        // Remove the lock when it is not needed anymore, and update local file list accordingly
            // How?
    }
}
