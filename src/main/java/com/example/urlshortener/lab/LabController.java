package com.example.urlshortener.lab;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LabController {

    @GetMapping("/")
    public String lab() {
        return "lab";
    }
}
