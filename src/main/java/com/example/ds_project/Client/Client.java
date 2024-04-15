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
    Inet4Address currentIP;
    int currentID;
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

    public int[] requestLinkIds()
    {
        String getUrl = "http://"+namingServerIP+"/ns/giveLinkID";
        RestTemplate restTemplate = new RestTemplate();

        // Make the GET request
        ResponseEntity<int[]> responseEntity = restTemplate.getForEntity(getUrl, int[].class);

        // Extract the response body
        int[] linkIds = responseEntity.getBody();

        // Return the link IDs
        return linkIds;
    }

    public Inet4Address[] requesLinkIPs(int[] linkID)
    {
        // namingServer.getIP(linkID[0])
        return null;
    }

    @Override
    public void shutDown()
    {
        int[] linkIds = requestLinkIds();
        int prevID = linkIds[0];
        int nextID = linkIds[1];

        Inet4Address[] linkIPs = requesLinkIPs(linkIds);
        Inet4Address nextNodeIP = linkIPs[0];
        Inet4Address prevNodeIP = linkIPs[1];

        // Send the previous ID to the next node
        sendLinkID(nextNodeIP, prevID, currentID);

        // Send the next ID to the previous node
        sendLinkID(prevNodeIP, currentID, nextID);

        // Remove from the naming server
        removeFromNS(namingServerIP, currentIP);
    }

    @Override
    public void sendLinkID(Inet4Address nodeIP, int startID, int otherID)
    {
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
        if (statusCode == HttpStatus.OK) {System.out.println("Update successful");}
        else {System.err.println("Update failed with status code: " + statusCode);}
    }

    public void receiveLinkID(int prevID, int nextID)
    {
        if(prevID == currentID)
        {
            this.nextID = nextID;
        }
        else if (nextID == currentID)
        {
            this.prevID = prevID;
        }
    }

    @Override
    public void removeFromNS(Inet4Address nsIP, Inet4Address nodeIP) {

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
