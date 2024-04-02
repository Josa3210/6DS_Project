package com.example.ds_project.Client;

import com.example.ds_project.namingServer.NamingServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.Inet4Address;

@SpringBootApplication
public class Client implements I_Client {
    public static void main(String[] args) {
        SpringApplication.run(NamingServer.class, args);
    }

    @Override
    public int computeHash(String s) {
        int p = 59;
        int m = 10000009;
        int hash_value = 0;
        int p_pow = 1;

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                hash_value = (hash_value + Integer.parseInt(String.valueOf(c)) * p_pow) % m;
            } else {
                hash_value = (hash_value + (c - 'a' + 1) * p_pow) % m;
            }
            p_pow = (p_pow * p) % m;
        }
        return hash_value;
    }

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
    @Override
    public void habari() {

    }

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
    @Override
    public void karibu() {

    }

    // Use "receiveLinkID" and "sendLinkID" from Shutdown

    /*Shutdown*/

    /**
     * Shutdown the client with informing the other nodes + NS
     */
    @Override
    public void shutDown() {

    }

    /**
     * Send the nextID or previousID to the other host.
     * <p>
     * If startID == nodeID than otherID = nextID
     * If otherID == nodeID than startID = prevID
     * </p>
     */
    @Override
    public void sendLinkID(Inet4Address nodeIP, int startID, int otherID) {

    }

    /**
     * Receive the nextID or previousID from other node.
     * <p>
     * If startID == currentID than otherID = nextID
     * If otherID == currentID than startID = prevID
     * </p>
     */
    @Override
    public void receiveLinkID() {

    }

    /**
     * Remove node with nodeIP from namingServer
     * <p>
     * Can also give own IP to be removed
     * </p>
     *
     * @param nsIP   IP of the namingServer
     * @param nodeIP IP of the node to be removed
     */
    @Override
    public void removeFromNS(Inet4Address nsIP, Inet4Address nodeIP) {

    }

    /*Failure*/

    /**
     * Check for connection with other host
     *
     * @param hostIP IP of the host to reach
     * @return
     */
    @Override
    public void ping(Inet4Address hostIP) {
        
    }

    /**
     * Reaction to a failure during communication with another node.
     *
     * @param hostIP
     */
    @Override
    public void removeFromNetwork(Inet4Address hostIP) {

    }
}
