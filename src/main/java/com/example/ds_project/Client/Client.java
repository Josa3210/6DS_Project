package com.example.ds_project.Client;

import com.example.ds_project.namingServer.NamingServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Client implements I_Client
{
    int currentID = computeHash("node1");
    Inet4Address currentIP = requestLinkIPs(currentID);
    int nextID, prevID;
    Inet4Address namingServerIP;

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

    @Override
    public void habari() {

    }

    @Override
    public void karibu() {

    }

    /**
     * Requests the ID's from the previous and next node from the naming server using REST
     * URI : http://[namingServerIP]/ns/giveLinkID
     * @return int[] of length 2, int[0] = previous node id, int[1] = next node id
     */
    @Override
    public int[] requestLinkIds()
    {
        String getUrl = "http://" + namingServerIP + ":8080/ns/giveLinkID";

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<int[]> responseEntity = restTemplate.getForEntity(getUrl, int[].class);

        return responseEntity.getBody();
    }

    /**
     * Requests the ip from the naming server by id
     * @param linkID the ID of the node
     * @return Inet4Address of the requested node
     */
    @Override
    public Inet4Address requestLinkIPs(int linkID)
    {
        String getUrl = "http://" + namingServerIP + ":8080/ns/getIP";

        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.getForObject(getUrl, Inet4Address.class);
    }

    /**
     * Shuts down the node by sending the previous and next node id's to the corresponding nodes
     * and removing from the naming server
     */
    @Override
    public void shutDown()
    {
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

    /**
     * Sends the previous and next id to the corresponding client, so it can update its id's
     * URI : http://[nodeIP]/shutdown/updateID
     * @param nodeIP the IP of the receiving client
     * @param startID the previous ID or the node ID
     * @param otherID the next ID or the node ID
     */
    @Override
    public void sendLinkID(Inet4Address nodeIP, int startID, int otherID)
    {
        String postUrl = "http://" + nodeIP + ":8080/shutdown/updateID";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>()
        {{
            put("prevID", startID);
            put("nextID", otherID);
        }};

        // Create the request entity with headers and body
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Send the POST request
        ResponseEntity<Void> responseEntity = restTemplate.postForEntity(postUrl, requestEntity, Void.class);

        HttpStatusCode statusCode = responseEntity.getStatusCode();
        if (statusCode == HttpStatus.OK) {System.out.println("Update successful");}
        else {System.err.println("Update failed with status code: " + statusCode);}
    }

    /**
     * Updates the next and previous id
     * When the previous id is equal to the client id, the next id will be updated
     * When the next id is equal to the client id, the previous id will be updated
     * @param prevID the previous id or client id
     * @param nextID the next id or client id
     */
    @Override
    public void receiveLinkID(int prevID, int nextID)
    {
        this.nextID = prevID == currentID ? nextID : currentID;
        this.prevID = nextID == currentID ? prevID : currentID;
    }

    @Override
    public void removeFromNS(Inet4Address nsIP, Inet4Address nodeIP)
    {
        String postUrl = "http://" + nsIP +  ":8080/ns/removeNode";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create the request body
        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("failedNode", nodeIP);

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
    public void ping(Inet4Address hostIP) {

    }

    @Override
    public void removeFromNetwork(Inet4Address hostIP) {

    }

    public void setNextID(int nextID) {
        this.nextID = nextID;
    }

    public void setPrevID(int prevID) {
        this.prevID = prevID;
    }
}
