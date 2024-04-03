package com.example.node.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestControllerFailure {
    @GetMapping("/test")
    public String TestConnection(@RequestParam String testString) {
        return ("Test Communication : " + testString + "\n");
    }

}
