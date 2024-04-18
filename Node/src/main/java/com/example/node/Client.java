package com.example.node;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.web.client.RestClient;

import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.EventListener;

public class Client implements I_Client {

    RestClient restClient;
    int currentID, nextID, prevID;
    private String hostname;
    private static Config config;
    private static String multicast_address = "224.2.2.5";
    private static HazelcastInstance hazelcastInstance;
    private static String IPaddress;

    private static MembershipListener event_listener;

    public Client(String hostname) {
        try{
        event_listener = new ClusterMemberShipListener();
        Client.CreateConfig();
        this.hostname = hostname;
        this.IPaddress = Inet4Address.getLocalHost().getHostAddress();
        hazelcastInstance.getMap("mapIP").put(IPaddress, this.hostname);
        this.restClient = RestClient.create();
        }

        catch (FileNotFoundException e){
        System.err.println(e.getMessage());
    } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }

    public class ClusterMemberShipListener implements MembershipListener {
        public void memberAdded(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/")+1, s.indexOf(":"));
            System.err.println(computeHash(s));
            System.err.println(s);
        }
        public void memberRemoved(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/")+1, s.indexOf(":"));
            System.err.println(s);
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
        config.addListenerConfig(new ListenerConfig(event_listener));
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
     * @param port
     * @return
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
     * @param hostIP
     */
    @Override
    public void removeFromNetwork(Inet4Address hostIP) {

    }
}
