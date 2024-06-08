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
import java.io.*;
import java.net.*;



import java.util.concurrent.TimeUnit;


public class Client implements I_Client {

    private static final String multicast_address = "224.2.2.5";
    private static ClusterMemberShipListener event_listener;
    private RestClient restClient;
    int currentID, nextID, prevID;
    private Config config;
    private Map<String, Inet4Address> ipMap;
    private String currentIP, namingServerIP;
    private Integer namingServerPort;
    private String hostname;
    private Logger logger;
    public String folderPath = "Data/node/Files";
    public int numberNodes = 0;
    public boolean startFileMonitor = false;
    public boolean isReceivedFile = false;
    boolean setupCompleted = false;
    private int portNumber = 80;
    public ServerSocket serverSocket;
    public Socket clientSocket;
    private Thread fileMonitorThread;


    /**
     * Constructor of the client
     * @param hostname the name of the client
     */
    public Client(String hostname) {
        try {
            event_listener = new ClusterMemberShipListener();
            this.config = createConfig();
            this.hostname = hostname;
            this.restClient = RestClient.create();
            this.currentIP = Inet4Address.getByName(Inet4Address.getLocalHost().getHostAddress()).getHostAddress();

            // We make a new logger file that keeps track of changes in the 'Files' map
            this.logger = new Logger(hostname); // We create a logger to keep track of the replication
            logger.load();
            fileMonitorThread = new Thread(new FileMonitor(this));
            }
        catch (FileNotFoundException | UnknownHostException e) { throw new RuntimeException(e); }
    }

    private Config createConfig() throws FileNotFoundException {

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
        return config;
    }

    public void ReceiveFile(String filepath, DataInputStream dataInputStream){

        try {

            isReceivedFile = true;
            int bytes = 0;
            FileOutputStream fileOutputStream
                    = new FileOutputStream(filepath);
            long size
                    = dataInputStream.readLong(); // read file size
            byte[] buffer = new byte[4 * 1024];
            while (size > 0
                    && (bytes = dataInputStream.read(
                    buffer, 0,
                    (int) Math.min(buffer.length, size)))
                    != -1) {
                // Here we write the file using write method
                fileOutputStream.write(buffer, 0, bytes);
                size -= bytes; // read upto file size
            }
            // Here we received file
            System.out.println("File is Received");
            fileOutputStream.close();
            this.serverSocket.close();
        }catch(IOException e1){
            System.err.println(e1.getMessage());
        }
    }


    public void SendFile(String filepath) {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(this.clientSocket.getOutputStream());
            int bytes = 0;
            // Open the File where he located in your pc
            File file = new File(filepath);
            FileInputStream fileInputStream
                    = new FileInputStream(file);
            // Here we send the File to Server
            dataOutputStream.writeLong(file.length());
            // Here we  break file into chunks
            byte[] buffer = new byte[4 * 1024];
            while ((bytes = fileInputStream.read(buffer))
                    != -1) {
                // Send the file to Server Socket
                dataOutputStream.write(buffer, 0, bytes);
                dataOutputStream.flush();
            }
            // close the file here
            System.out.println("File was sent");
            fileInputStream.close();
            clientSocket.close();
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
        }
    }

    public Thread getFileMonitorThread() {
        return fileMonitorThread;
    }

    /**
     * Writes information about the client to the console:
     * - Current, next and previous ID
     * - IP and Port of the naming server
     */
    @Override
    public void printLinkIds()
    {
        System.out.println(">> Client ID's");
        System.out.println("* Current ID: " + this.currentID);
        System.out.println("* Next ID: " + this.nextID);
        System.out.println("* Previous ID: " + this.prevID);
        System.out.println("* IP + port NS: " + this.namingServerIP + " + " + this.namingServerPort);
    }

    /**
     * Post Constructor of the client
     */
    @PostConstruct
    public void init()
    {
        habari();
        this.currentID = computeHash(hostname);
        this.nextID = Integer.MAX_VALUE;
        this.prevID = Integer.MIN_VALUE;
    }

    /**
     * This method computes a hash value for a given string by iterating through
     * each character and applying a polynomial rolling hash function. The hash
     * value is calculated as (sum of character values * p_pow) % m, where p is a
     * prime number, m is a large prime number, and p_pow is the current power of p.
     *
     * @param s the input string for which the hash value needs to be computed
     * @return the computed hash value for the input string
     */
    @Override
    public int computeHash(String s)
    {
        int p = 59;
        int m = 10000009;
        int hash_value = 0;
        int p_pow = 1;

        for (char c : s.toCharArray())
        {
            if (Character.isDigit(c)) hash_value = (hash_value + Integer.parseInt(String.valueOf(c)) * p_pow) % m;
            else hash_value = (hash_value + (c - 'a' + 1) * p_pow) % m;

            p_pow = (p_pow * p) % m;
        }
        return hash_value;
    }

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
    public void habari()
    {
        // Joins multicast group
        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(this.config); // Creates a new Hazelcast instance with the provided configuration.
    }

    /**
     * This method will set the previous and next node ID's
     * and ask for the naming server to add the client
     * @param nrNodes the size of the cluster in the network
     * @param namingServerIP the IP of the naming server
     * @param namingServerPort the port of the naming server
     */
    public void setupClient(int nrNodes, String namingServerIP, int namingServerPort)
    {
        this.namingServerIP = namingServerIP;
        this.namingServerPort = namingServerPort;

        addNameToNS();

        int[] ids = requestLinkIds();
        nextID = nrNodes == 1 ? currentID :  ids[1];
        prevID = nrNodes == 1 ? currentID :  ids[0];

        sendLinkID(nextID);
        sendLinkID(prevID);
    }

    /**
     * This method sends a Post REST request to the naming server
     * asking to add itself to the network
     */
    private void addNameToNS()
    {
        String postUrl = "http://" + namingServerIP + ":" + namingServerPort + "/ns/addNode";

        System.out.println(">> Sending REST Post (addNameToNS)");
        System.out.println("* Post url: " + postUrl);
        System.out.println("* IP + Name: " + currentIP + " + " + hostname);

        RestTemplate restTemplate = new RestTemplate();

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>()
        {{
            put("ip", currentIP);
            put("name", hostname);
        }};

        System.out.println("Requesting...");

        // Send the POST request
        restTemplate.postForEntity(postUrl, requestBody, Void.class);
    }

    /*Discovery + Bootstrap*/

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
    public void karibu(int hash)
    {
        if ((this.currentID < hash) && (hash < this.nextID)) this.nextID = hash;
        else if ((this.prevID < hash) && (hash < this.currentID)) this.prevID = hash;
    }

    /**
     * Returns the previous and next ID of the client itself
     * @return previous ID (int[0]) and next ID (int[1])
     */
    @Override
    public int[] requestLinkIds() { return requestLinkIds(currentID); }

    /**
     * Returns the previous and next ID of the requested client
     * @param requestID the ID of the requested client
     * @return previous ID (int[0]) and next ID (int[1])
     */
    @Override
    public int[] requestLinkIds(int requestID)
    {
        String getUrl = "http://" + namingServerIP + ":" + namingServerPort + "/ns/giveLinkID/" + requestID;
        RestTemplate restTemplate = new RestTemplate();

        System.out.println(">> Sending REST Get (requestLinkIds)");
        System.out.println("* Get URL: " + getUrl);

        ResponseEntity<int[]> response = restTemplate.getForEntity(getUrl, int[].class);
        return response.getBody();
    }

    /**
     * Returns the IP of a node using the ID
     * @param nodeID the ID of the requested node
     * @return the IP of the requested node in a string
     */
    @Override
    public String requestIP(int nodeID)
    {
        String getUrl = "http://" + namingServerIP + ":" + namingServerPort + "/ns/getIp/" + nodeID;

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(getUrl, String.class);

        return response.getBody();
    }

    /**
     * This method will shut the client down by removing itself from the naming server
     * and sending the previous and next ID to the corresponding nodes
     */
    @Override
    public void shutDown()
    {
        int[] linkIds = requestLinkIds();
        int prevID = linkIds[0];
        int nextID = linkIds[1];

        // Remove from the naming server
        removeFromNS();

        sendLinkID(nextID);
        sendLinkID(prevID);

        ClientApplication.exitApplication();
    }

    /**
     * Requests a node to update it's previous and next ID
     * @param nodeID the requested nodes ID
     */
    @Override
    public void sendLinkID(int nodeID)
    {
        String nodeIP = requestIP(nodeID);
        String postUrl = "http://" + nodeIP + ":" + namingServerPort + "/shutdown/updateID";
        System.out.println(">> Sending REST Post (sendLinkID)");

        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> requestBody = new HashMap<>();

        try
        {
            ResponseEntity<Void> requestEntity = restTemplate.postForEntity(postUrl, requestBody, Void.class);
            if (!requestEntity.getStatusCode().is2xxSuccessful()) removeFromNetwork(nodeID);
        }
        catch (Exception e){ removeFromNetwork(nodeID); }
    }

    /**
     * This method will update the client's previous and next ID
     * @param prevID the previous ID or current ID
     * @param nextID the next ID or current ID
     */
    @Override
    public void receiveLinkID(int prevID, int nextID)
    {
        System.out.println(">> Updating prev and next ID : current:" + currentID + ", next ID: " + nextID + ", prev ID: " + prevID);
        this.nextID = prevID == currentID ? nextID : this.nextID;
        this.prevID = nextID == currentID ? prevID : this.prevID;
    }

    /**
     * Remove node with nodeIP from namingServer
     * <p>
     * Can also give own IP to be removed
     * </p>
     */
    @Override
    public void removeFromNS() {removeFromNS(currentID);}

    @Override
    public void removeFromNS(int removeID)
    {
        String postUrl = "http://" + namingServerIP + ":" + namingServerPort+ "/ns/removeNode";

        System.out.println(">> Sending REST Post (removeFromNS)");
        System.out.println("* Post URL: " + postUrl);
        System.out.println("* Node ID: " + removeID);

        RestTemplate restTemplate = new RestTemplate();

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>()
        {{
            put("nodeID", removeID);
        }};

        // Send the POST request
        restTemplate.postForEntity(postUrl, requestBody, Void.class);

        // if  a node gets removed from the namingserver, the files replicated on this node should be
        // moved to  the previous node, that will become the owner of the files
        // We first remove the current entry with the original IP
        if (removeID == currentID) {
            if (prevID != currentID) {
                logger.load();
                postUrl = "http://" + namingServerIP + ":8080/ns/shutdown";
                restTemplate = new RestTemplate();
                requestBody.clear();
                requestBody.put("PrevID", prevID);
                requestBody.put("nodeMap", logger.getNodeMap());
                System.out.println("nodemap logger: " + logger.getNodeMap());
                requestBody.put("fileMap", logger.getFileMap());
                System.out.println("filemap logger: " + logger.getFileMap());
                requestBody.put("originalIP", currentIP);
                restTemplate.postForEntity(postUrl, requestBody, Void.class);
            }
        }
    }

    /**
     * Check for connection with other host
     * @param nodeID
     */
    @Override
    public void ping(int nodeID)
    {
        String nodeIP = requestIP(nodeID);

        String uri = "http://" + nodeIP + ":8080/test" + "?testString=test";
        System.out.println(">> Pinging: " + uri);
        RestTemplate restTemplate = new RestTemplate();
        try
        {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(uri, String.class);
            System.out.println("* Response: " + responseEntity.getBody());

            if (!responseEntity.getStatusCode().is2xxSuccessful()) removeFromNetwork(nodeID);
        }
        catch (Exception e) { removeFromNetwork(nodeID); }
    }


    /**
     * Reaction to a failure during communication with another node.
     *
     * @param failedID
     */
    @Override
    public void removeFromNetwork(int failedID)
    {
        System.out.println(">> Removing client from network");

        int[] ids = requestLinkIds(failedID);

        // remove failed node from network
        removeFromNS(failedID);

        // Send prevID to nextIP and nextID to prevID
        sendLinkID(ids[0]);
        sendLinkID(ids[1]);

    }

    @Override
    public void getName() { System.out.println(this.hostname); }

    public void setNextID(int nextID) { this.nextID = nextID; }
    public void setPrevID(int prevID) { this.prevID = prevID; }
    public String getHostname() { return hostname; }
    public Logger getLogger() { return logger; }
    public String getCurrentIP() {
        return currentIP;
    }

    @Override
    public void reportFilenameToNamingServer(String filename)
    {
        // Prepare the URL for reporting the hash value to the naming server
        String postUrl = "http://" + namingServerIP + ":8080/ns/reportFileName";
        Map<String, Object> requestBody = new HashMap<>();
        System.out.println("operation: " + operation);
        requestBody.put("filename", filename);
        requestBody.put("filepath", filePath);
        System.out.println(filePath);
        requestBody.put("ip", currentIP);
        requestBody.put("operation", operation);
        requestBody.put("ID", nextID);
        System.out.println("prev: " + prevID + ", current ID: " + currentID + "next ID " + nextID) ;

        // Make an HTTP POST request to report the hash value
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestBody, Void.class);

        if (responseEntity.getStatusCode().is2xxSuccessful()) System.out.println("Hash value reported to naming server for file: " + filename);
        else System.err.println("Failed to report hash value to naming server for file: " + filename);
    }

    public void sendReplicatedFile(Inet4Address originalIP, String filepath) throws IOException {
        this.serverSocket = new ServerSocket(5000);
        String url = "http://" +originalIP.getHostAddress()+":8080/OpenTCPConnection";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("replicated ip", InetAddress.getLocalHost().getHostAddress());
        String completed = String.valueOf(restTemplate.postForEntity(url, requestBody, String.class));
        System.out.println(completed);
        this.clientSocket = this.serverSocket.accept();
        DataInputStream dataInputStream = new DataInputStream(this.clientSocket.getInputStream());
        url = "http://" + originalIP.getHostAddress()+ ":8080/StartFileTransfer";
        requestBody.clear();
        requestBody.put("filepath", filepath);
        restTemplate.postForEntity(url, requestBody, Void.class);
        this.ReceiveFile(filepath,dataInputStream);
    }

    public int getPrevID() {
        return prevID;
    }

    public class ClusterMemberShipListener implements MembershipListener {
        public void memberAdded(MembershipEvent membershipEvent) {
            if (startFileMonitor){
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                startFileMonitor = false;
                Thread filemonitorthread = getFileMonitorThread();
                filemonitorthread.start();
            }
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));
            int hash = computeHash(s);
            karibu(hash);
        }

        public void memberRemoved(MembershipEvent membershipEvent) {
            String s = membershipEvent.getMember().getSocketAddress().toString();
            s = s.substring(s.indexOf("/") + 1, s.indexOf(":"));

        }
    }
}
