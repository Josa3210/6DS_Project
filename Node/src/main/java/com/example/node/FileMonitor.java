

package com.example.node;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


/**
 * This class represents a task for monitoring file system changes in the specified Files directory.
 *
 * It implements the Runnable interface to be executed in a separate thread.

**/

public class FileMonitor implements Runnable {

    Client client;

    public FileMonitor(Client client) {
        this.client = client;

    }

    public void run() {

        Path path = Paths.get(client.folderPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            System.err.println(e.getMessage());

            throw new RuntimeException(e);
        }
        System.out.println("folder created: " + client.folderPath);

        // Create a FileAlterationObserver for the specified folder path (specified in the client)
        FileAlterationObserver observer = new FileAlterationObserver(client.folderPath);

        // Add a listener to handle file system events
        observer.addListener(new FileAlterationListenerAdaptor() {

            @Override
            public void onFileCreate(File file) {

                String filename = file.getName();
                String filepath = file.getPath();

                if (!filename.endsWith(".swp")) {
                    System.out.println("\nFile created: " + filename);
                    Logger logger = client.getLogger();
                    logger.load();
                    int hash = client.computeHash(filename);

                    try {
                        logger.put(hash, (Inet4Address) InetAddress.getByName(client.getCurrentIP()));
                    } catch (UnknownHostException e) {
                        throw new RuntimeException(e);
                    }

                    System.out.println("hash: " + hash + " current IP: " + client.getCurrentIP());

                    logger.putFile(hash, filepath);
                    client.getFileList().add(new NodeFileEntry(filename));
                    logger.save();

                    if (!client.isReceivedFile) { // if the file is locally made, we let the namingserver know
                        client.reportFilenameToNamingServer(file.getName(), filepath, 1); // Operation 1 --> file CREATE
                    }

                }

                client.isReceivedFile = false;  // Reset flag to false after file is received

            }

            @Override
            public void onFileDelete(File file) {

                String filename = file.getName();

                if (!filename.endsWith(".swp")) { // we don't look at temporary files

                    String filepath = file.getPath();
                    System.out.println("filepath: " + filepath);


                    // We remove the file from the logger
                    Logger logger = client.getLogger();
                    logger.load();
                    int hash = client.computeHash(filename);
                    String originalIP = logger.get(hash).getHostAddress();
                    System.out.println("original IP" + originalIP);
                    String currentIP = client.getCurrentIP();

                    if (originalIP.equals(currentIP) & !client.isReplicatedFile) // we check if the original IP of the file = current IP

                        // if this is the case, the current IP is the IP where the file got downloaded, so we need to make
                        // sure that the naming server gets noted about this so it can remove the replicated files too.
                        client.reportFilenameToNamingServer(filename, filepath, 2); // Operation 2 --> file DELETE on replicated node

                    client.getFileList().removeIf(entry -> filename.equals(entry.getFilename()));
                    logger.remove(hash); // We remove the hash from the logger.
                }
                client.isReplicatedFile = false;
            }
        });

        while (true) {  // Start monitoring the directory

            try {
                observer.checkAndNotify();
                Thread.sleep(1000); // Adjust sleep time as needed

            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }

    }
}