package com.example.node;

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import jakarta.annotation.PostConstruct;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.io.FileNotFoundException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Client implements I_Client {

    private static final String multicast_address = "224.2.2.5";
    RestClient restClient;
    int currentID, nextID, prevID;
    Map<String, Inet4Address> ipMap;
    private Inet4Address namingServerIP;
    Inet4Address currentIP;
    private Integer namingServerPort;
    private String hostname;

    private static ClusterMemberShipListener event_listener;

    public Client(String hostname) {
        try {
            event_listener = new ClusterMemberShipListener();
            Client.CreateConfig();
            this.hostname = hostname;
            this.restClient = RestClient.create();
            this.currentIP = (Inet4Address) InetAddress.getLocalHost();
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

    }

    public class ClusterMemberShipListener implements MembershipListener {
        public void memberAdded(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/")+1, s.indexOf(":"));
            int hash = computeHash(s);
            karibu(hash);
        }
        public void memberRemoved(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/")+1, s.indexOf(":"));

        }
    }


    private static void CreateConfig() throws FileNotFoundException {

        Config config = new Config();
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
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(config); // Creates a new Hazelcast instance with the provided configuration.
    }

    @PostConstruct
    public void init() {
        this.currentID = computeHash(hostname);
        this.nextID = Integer.MAX_VALUE;
        this.prevID = Integer.MIN_VALUE;
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
    public void karibu(int hash) {
        if((this.currentID < hash) && (hash < this.nextID)){
            this.nextID = hash;
        } else if ((this.prevID < hash) && (hash < this.currentID)) {
            this.prevID = hash;
        }
    }

    // Use "receiveLinkID" and "sendLinkID" from Shutdown

    /*Shutdown*/

    @Override
    public int[] requestLinkIds() {
        String getUrl = "http://" + namingServerIP + "/ns/giveLinkID";
        RestTemplate restTemplate = new RestTemplate();

        // Make the GET request
        ResponseEntity<int[]> responseEntity = restTemplate.getForEntity(getUrl, int[].class);

        // Return the link IDs
        return responseEntity.getBody();
    }

    @Override
    public Inet4Address requestLinkIPs(int linkID) {
        String getUrl = "http://" + namingServerIP + "/ns/getIP";

        RestTemplate restTemplate = new RestTemplate();

        Inet4Address ip = restTemplate.getForObject(getUrl, Inet4Address.class);

        return ip;
    }

    @Override
    public void shutDown() {
        int[] linkIds = requestLinkIds();
        int prevID = linkIds[0];
        int nextID = linkIds[1];

        Inet4Address nextNodeIP = requestLinkIPs(nextID);
        Inet4Address prevNodeIP = requestLinkIPs(prevID);

        // Send the previous ID to the next node
        sendLinkID(nextNodeIP, prevID, currentID);

        // Send the next ID to the previous node
        sendLinkID(prevNodeIP, currentID, nextID);

        // Remove from the naming server
        removeFromNS(namingServerIP, currentIP);
    }

    @Override
    public void sendLinkID(Inet4Address nodeIP, int startID, int otherID) {
        String postUrl = "http://" + nodeIP + "/shutdown/updateID";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("prevID", startID);
        requestBody.put("nextID", otherID);

        // Create the request entity with headers and body
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Send the POST request
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestEntity, Void.class);
        HttpStatusCode statusCode = responseEntity.getStatusCode();
        if (statusCode == HttpStatus.OK) {
            System.out.println("Update successful");
        } else {
            System.err.println("Update failed with status code: " + statusCode);
        }
    }

    @Override
    public void receiveLinkID(int prevID, int nextID) {
        this.nextID = prevID == currentID ? nextID : currentID;
        this.prevID = nextID == currentID ? prevID : currentID;
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
