

package com.example.node;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.util.Arrays;


/**
 * This class represents a task for monitoring file system changes in the specified Files directory.
 *
 * It implements the Runnable interface to be executed in a separate thread.

**/

public class FileMonitor implements Runnable {

    String folderPath;
    Client client;

    public FileMonitor(Client client, String folderPath) {
        this.client = client;
        this.folderPath = folderPath;
    }

    public void run() {

        // Create a FileAlterationObserver for the specified folder path (specified in the client)
        FileAlterationObserver observer = new FileAlterationObserver(folderPath);

        // Add a listener to handle file system events
        observer.addListener(new FileAlterationListenerAdaptor() {

            @Override
            public void onFileCreate(File file) {

                String filename = file.getName();
                System.out.println("\nFile created: " + filename);
                client.reportFilenameToNamingServer(file.getName());

            }

            @Override
            public void onFileDelete(File file) {

                    System.out.println("File deleted: " + file.getName());

            }
        });

        // Start monitoring the directory
        while (true) {
            System.out.println("checking Files ..");
            try {
                observer.checkAndNotify();
                Thread.sleep(1000); // Adjust sleep time as needed

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}