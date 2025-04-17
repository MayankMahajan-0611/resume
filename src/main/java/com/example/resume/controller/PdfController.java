package com.example.resume.controller;
import com.example.resume.services.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class PdfController {

    private final GeminiService geminiService;

    @Autowired
    public PdfController (GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/processPdf")
    public ResponseEntity<String> processPdf(@RequestParam("file") MultipartFile file, @RequestParam("prompt") String prompt) {
        try {
            String analysisResult = geminiService.processPdf(file, prompt);
            return ResponseEntity.ok(analysisResult);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("{\"error\": \"Could not process file: " + e.getMessage() + "\"}");
        }
    }
}