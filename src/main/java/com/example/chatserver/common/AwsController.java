package com.example.chatserver.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AwsController {

    @GetMapping("/")
    public String root() {
        return "OK";
    }
}
