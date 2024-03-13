package com.example.ds_project.Client;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Client {

    public void loadfile() {

        String imagePath = "kittler.jpg"; // Specify the path to your image file

        try {
            File file = new File(imagePath);
            BufferedImage image = ImageIO.read(file);

            // Do something with the image, e.g., display it or process it further
            System.out.println("Image width: " + image.getWidth());
            System.out.println("Image height: " + image.getHeight());
        } catch (IOException e) {
            System.err.println("Error reading the image: " + e.getMessage());
        }
    }

}