package com.example.node.Agents;

import com.example.node.Client;
import com.example.node.NodeFileEntry;
import jade.core.behaviours.CyclicBehaviour;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class SyncBehaviour extends CyclicBehaviour
{
    private final Client client;
    private final SyncAgent agent;

    public SyncBehaviour(Client client, SyncAgent agent)
    {
        System.out.println("^^^^Debugging Created SyncBehaviour");
        this.client = client;
        this.agent = agent;
    }

    @Override
    public void action()
    {
        if(agent.isActive())
        {
            System.out.println(">> SYNC AGENT");
            System.out.println(">> --------------------------");

            // REST ask for the sync agent list of the next node
            System.out.println("** Requesting Agent Files Next Node");

            String ipNextNode = client.requestIP(client.getNextID());

            String getUrl = "http://" + ipNextNode + ":8080/agents/sync"; // Failure Exception?
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<SyncAgent> response = restTemplate.getForEntity(getUrl, SyncAgent.class);

            SyncAgent nextNodeAgent = response.getBody();
            List<NodeFileEntry> nextNodeList = nextNodeAgent.getAgentFiles();

            for(NodeFileEntry nextNodeEntry : nextNodeList)
                System.out.println("* File Name: " + nextNodeEntry.getFilename() + " - Locked: " + nextNodeEntry.isLocked());

            agent.setAgentFiles(nextNodeList);

            // Listing all files owned by the node at which this agent runs,
            System.out.println("** All owned files of the client:");
            for(NodeFileEntry e : client.getFileList())
                System.out.println("* File Name: " + e.getFilename() + " - Locked: " + e.isLocked());

            // If one of the owned files is not added to the list, the list needs to be updated
            System.out.println("** Updating Agent List");
            for(NodeFileEntry entry : client.getFileList())
                updateList(entry.getFilename(), agent.getAgentFiles());

            // Update the list stored by the node based on the agent’s list
            System.out.println("** Updating Client List");
            for(NodeFileEntry entry : agent.getAgentFiles())
                updateList(entry.getFilename(), client.getFileList());

            // If there is a lock request on the current node, and the file is not locked on the agent’s list, locking should be
            // enabled on the node and the list should be synchronized accordingly
            // Remove the lock when it is not needed anymore, and update local file list accordingly
            System.out.println("** Update Lock on files agent list");
            for(NodeFileEntry entry : client.getFileList())
                updateLock(entry, agent.getAgentFiles());


            System.out.println(">> --------------------------");
        }
    }

    /**
     * Updates the lock variable on the sync agent list
     * @param entry the entry of the client to update
     * @param fileList the list on the agent to check
     */
    private void updateLock(NodeFileEntry entry, List<NodeFileEntry> fileList)
    {
        for(NodeFileEntry file : fileList)
        {
            if(file.getFilename().equals(entry.getFilename()) && file.isLocked() != entry.isLocked())
            {
                file.setLocked(entry.isLocked());
                System.out.println("* File locking: " + entry.isLocked() + " - file name: " + entry.getFilename());
            }
        }
    }

    /**
     * Adds a file to a list if the file is not in the list
     * @param fileName the name of the file to check
     * @param fileList the list of files to update
     */
    private void updateList(String fileName, List<NodeFileEntry> fileList)
    {
        if(fileList.stream().noneMatch(file -> fileName.equals(file.getFilename())))
        {
            System.out.println("* File not found: " + fileName + " - Adding to List");
            fileList.add(new NodeFileEntry(fileName));
        }
    }
}
