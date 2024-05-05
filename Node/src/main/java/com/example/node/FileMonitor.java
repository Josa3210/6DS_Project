

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

    private final String folderPath;
    private final Client client;

    public FileMonitor(Client client, String folderPath) {
        this.client = client;
        this.folderPath = folderPath;
    }

    public void run() {

        // Create a FileAlterationObserver for the specified folder path (specified in the client)
        FileAlterationObserver observer = new FileAlterationObserver(folderPath);

        // Perform an initial scan of the directory, to check there are already files saved ..
        File[] existingFiles = new File(folderPath).listFiles();
        System.out.println("Existing files detected: " +  Arrays.toString(existingFiles));

        // if the client setup is completed, we can start with reporting the files to the namingserver
        if (client.isSetupCompleted()){
            System.out.println("setup completed");
            if (existingFiles != null) {

                for (File file : existingFiles) {
                    System.out.println("Existing files found! -------------------------------------------------");

                    // Calculate hash and report to naming server for existing files
                    client.reportFilenameToNamingServer(file.getName());
                }
            }
        }
        else{

            System.out.println("Setup not completed!");
        }

        // Add a listener to handle file system events
        observer.addListener(new FileAlterationListenerAdaptor() {

            @Override
            public void onFileCreate(File file) {

                // Calculate hash and report to naming server when a new file is created

                if (client.isSetupCompleted()) {
                    String filename = file.getName();
                    System.out.println("File created: " + filename);

                    int hash = client.computeHash(filename);
                    client.reportFilenameToNamingServer(file.getName());
                }
            }

            @Override
            public void onFileDelete(File file) {

                if (client.isSetupCompleted()) {
                    // Handle file deletion event if needed
                    System.out.println("File deleted: " + file.getName());
                }
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