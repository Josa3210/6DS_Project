package com.example.node;

import java.util.ArrayList;

public class ClientFilesResponse
{
    private ArrayList<ArrayList<String>> clientFiles;

    public ClientFilesResponse() {
    }

    public ArrayList<ArrayList<String>> getClientFiles() {
        return clientFiles;
    }

    public void setClientFiles(ArrayList<ArrayList<String>> clientFiles) {
        this.clientFiles = clientFiles;
    }
}
