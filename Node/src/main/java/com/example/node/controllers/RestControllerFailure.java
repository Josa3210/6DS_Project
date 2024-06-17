package com.example.node.controllers;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class RestControllerFailure
{
    @GetMapping("/test")
    public String TestConnection(@RequestParam String testString)
    {
        return (">> Test Communication : " + testString + "\n");
    }

    @PostMapping("/testPost")
    public String TestPost(@RequestBody Map<String, Object> request)
    {
        return (">> Params received : " + request.get("testObject"));
    }

}
