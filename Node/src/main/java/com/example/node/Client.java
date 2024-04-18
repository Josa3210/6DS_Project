package com.example.node;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Client implements I_Client {

    RestClient restClient;
    int currentID, nextID, prevID;
    private Inet4Address namingServerIP;
    private Integer namingServerPort;
    Map<String, Inet4Address> ipMap;
    private String hostname;
    private static Config config;
    private static String multicast_address = "224.2.2.5";
    private static HazelcastInstance hazelcastInstance;

    public Client(String hostname) {
        try{
        Client.CreateConfig();
        this.hostname = hostname;
        this.restClient = RestClient.create();
        }

        catch (FileNotFoundException e){
        System.err.println(e.getMessage());
    }

    }

    private static void CreateConfig() throws FileNotFoundException {

        config = new Config();
        config.getJetConfig().setEnabled(true);
        config.setClusterName("testCluster");
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(5701);
        networkConfig.setPortAutoIncrement(true); // Allows automatic increment of port numbers if necessary.
        networkConfig.addOutboundPortDefinition("5900-5915");
        networkConfig.getRestApiConfig().setEnabled(true); // Enables the REST API for the network configuration.
        JoinConfig joinConfig = networkConfig.getJoin();
        joinConfig.getMulticastConfig().setEnabled(true); // Enables multicast for node discovery
        joinConfig.getMulticastConfig().setMulticastGroup(multicast_address); // Sets the multicast group address.
        joinConfig.getMulticastConfig().setMulticastPort(54321);
        config.getManagementCenterConfig().setConsoleEnabled(true); // Enables the management center console.
        hazelcastInstance =  Hazelcast.newHazelcastInstance(config); // Creates a new Hazelcast instance with the provided configuration.
    }

    @PostConstruct
    public void init() {
        this.currentID = computeHash(hostname);
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
     * @param port   port to ping to
     */
    @Override
    public void ping(Inet4Address hostIP, String port) {

        String uri = "http:/" + hostIP + ":" + port + "/test" + "?testString=test";
        System.out.println("Pinging " + uri);
        String answer = restClient.get().uri(uri).retrieve().body(String.class);
        System.out.println(answer);
    }

    /**
     * Reaction to a failure during communication with another node.
     *
     * @param failedNode
     */
    @Override
    public void removeFromNetwork(String failedNode) {
        int nextID = 1;
        int prevID = 2;
        // Get linkID's from NS (now with examples because function in NS is not yet made)
        // nextID, prevID = APIGetLinkIDs(failedIP)

        // Get IP addresses of the link IDs
        ResponseEntity<String> response;
        Inet4Address nextIP, prevIP;
        try {
            response = restClient.post().uri("http:/" + this.namingServerIP + ":" + this.namingServerPort + "/project/getIP").body(nextID).retrieve().toEntity(String.class);
            nextIP = (Inet4Address) InetAddress.getByName(response.getBody());

            response = restClient.post().uri("http:/" + this.namingServerIP + ":" + this.namingServerPort + "/project/getIP").body(nextID).retrieve().toEntity(String.class);
            prevIP = (Inet4Address) InetAddress.getByName(response.getBody());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        // Send prevID to nextIP and nextID to prevID
        sendLinkID(nextIP, prevID, nextID);
        sendLinkID(prevIP, prevID, nextID);

        // Remove failed node from network
        restClient.post().uri("http:/" + this.namingServerIP + ":" + this.namingServerPort + "/project/removeNode").body(failedNode).retrieve().toBodilessEntity();

    }
}
