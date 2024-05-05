

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

                // Calculate hash and report to naming server when a new file is created
                //if (client.namingServerIP != null) {
                    String filename = file.getName();
                    System.out.println("File created: " + filename);
                    client.reportFilenameToNamingServer(file.getName());
                //}
            }

            @Override
            public void onFileDelete(File file) {

                //if (client.namingServerIP != null) {
                    // Handle file deletion event if needed
                    System.out.println("File deleted: " + file.getName());
                //}
            }
        });

        // Start monitoring the directory
        while (true) {
            try {
                observer.checkAndNotify();
                Thread.sleep(1000); // Adjust sleep time as needed

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}