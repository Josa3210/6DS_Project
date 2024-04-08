package com.example.node;

import java.net.Inet4Address;

public interface I_Client {

    int computeHash(String s);

    /*Discovery + Bootstrap*/

    /**
     * Send message to every other node when joining the network
     * "Habari" is "Hello" in Swahili
     *
     * <p>
     * Calculate own hash and put it as currentID
     * Send own name and IP-address to everyone using MC
     * </p>
     *
     * <p>
     * Receive message from naming server with #node in network:
     * #nodes = 1 -> nextID & prevID = currentID
     * #nodes > 1 -> will receive message from other nodes with 2 ID's
     * </p>
     */
    void habari();

    /**
     * Reply to "Habari" of other node
     * "Karibu" is "Welcome" in Swahili
     *
     * <p>
     * Calculate hash entering node (enteringID)
     * Check if entering node is new previous/next node (look slides)
     * Yes: answer (UC) to entering node with (startID, otherID) -> dependent of hash
     * No: do nothing
     * </p>
     * <p>
     */
    void karibu();

    // Use "receiveLinkID" and "sendLinkID" from Shutdown

    /*Shutdown*/

    /**
     * Shutdown the client with informing the other nodes + NS
     */
    void shutDown();

    /**
     * Send the nextID or previousID to the other host.
     * <p>
     * If startID == nodeID than otherID = nextID
     * If otherID == nodeID than startID = prevID
     * </p>
     */
    void sendLinkID(Inet4Address nodeIP, int startID, int otherID);

    /**
     * Receive the nextID or previousID from other node.
     * <p>
     * If startID == currentID than otherID = nextID
     * If otherID == currentID than startID = prevID
     * </p>
     */
    void receiveLinkID();

    /**
     * Remove node with nodeIP from namingServer
     * <p>
     * Can also give own IP to be removed
     * </p>
     *
     * @param nsIP   IP of the namingServer
     * @param nodeIP IP of the node to be removed
     */
    void removeFromNS(Inet4Address nsIP, Inet4Address nodeIP);

    /*Failure*/

    /**
     * Check for connection with other host
     *
     * @param hostIP IP of the host to reach
     * @param arg
     * @return
     */
    void ping(Inet4Address hostIP, String arg);

    /**
     * Reaction to a failure during communication with another node.
     *
     * @param failedID
     */
    void removeFromNetwork(int failedID);
}
