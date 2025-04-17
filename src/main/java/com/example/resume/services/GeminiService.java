package com.example.resume.services;
import com.example.resume.config.GeminiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiService(GeminiConfig geminiConfig, ObjectMapper objectMapper) {
        this.geminiConfig = geminiConfig;
        this.objectMapper = objectMapper;
    }

    public String processPdf(MultipartFile file, String prompt) throws IOException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "Analyze the resume based on the following criteria and suggest improvements:\n" +
                "1. Education: Score (out of 10) and suggest relevant degrees, certifications, or courses.\n" +
                "2. Work Experience: Score (out of 10) and suggest relevant positions, projects, and internships.\n" +
                "3. Skills: Highlight missing skills based on the job description, score (out of 10) the existing skills, and suggest how to acquire missing skills (e.g., courses, certifications).\n" +
                "4. Keywords: Identify missing keywords from the job description, score (out of 10) the presence of relevant keywords, and suggest how to incorporate missing keywords.\n" +
                "5. Achievements: Recommend adding notable accomplishments and awards.\n" +
                "6. Formatting: Suggest ways to improve the resume's format for better readability.\n" +
                "\n" +
                "Provide a total score (out of 60) and detailed feedback for improvement for each category, including real-time recommendations for courses, projects, internships, and other relevant activities.\n" +
                "\n" +
                "Output Format (must be valid JSON):\n" +
                "\n" +
                "{\n" +
                "  \"education\": {\n" +
                "    \"score\": <integer>,\n" +
                "    \"suggestions\": \"<string>\"\n" +
                "  },\n" +
                "  \"workExperience\": {\n" +
                "    \"score\": <integer>,\n" +
                "    \"suggestions\": \"<string>\"\n" +
                "  },\n" +
                "  \"skills\": {\n" +
                "    \"missingSkills\": \"<string>\",\n" +
                "    \"score\": <integer>,\n" +
                "    \"suggestions\": \"<string>\"\n" +
                "  },\n" +
                "  \"keywords\": {\n" +
                "    \"missingKeywords\": \"<string>\",\n" +
                "    \"score\": <integer>,\n" +
                "    \"suggestions\": \"<string>\"\n" +
                "  },\n" +
                "  \"achievements\": {\n" +
                "    \"suggestions\": \"<string>\"\n" +
                "  },\n" +
                "  \"formatting\": {\n" +
                "    \"suggestions\": \"<string>\"\n" +
                "  },\n" +
                "  \"totalScore\": <integer>,\n" +
                "  \"detailedFeedback\": \"<string>\"\n" +
                "}";

        byte[] fileBytes = file.getBytes();
        String base64EncodedPdf = Base64.getEncoder().encodeToString(fileBytes);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("inlineData", Map.of("mimeType", "application/pdf", "data", base64EncodedPdf)),
                        Map.of("text", "Job Description: " + prompt + "\n\n" + systemPrompt)
                ))
        ));

        String apiUrlWithKey = geminiConfig.getApiUrl() + geminiConfig.getApiKey();

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(apiUrlWithKey, HttpMethod.POST, entity, Map.class);
        if (response.getBody() != null && response.getBody().containsKey("candidates")) {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
            if (!candidates.isEmpty() && candidates.get(0).containsKey("content")) {
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                if (content.containsKey("parts")) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (!parts.isEmpty() && parts.get(0).containsKey("text")) {
                        String geminiResponse = (String) parts.get(0).get("text");

                        // Remove markdown formatting if present
                        geminiResponse = geminiResponse.replace("```json", "").replace("```", "").trim();

                        try {
                            // Attempt to parse the cleaned response as JSON
                            objectMapper.readTree(geminiResponse);
                            return geminiResponse;
                        } catch (com.fasterxml.jackson.core.JsonParseException e) {
                            // Handle cases where the response is still not directly JSON
                            // (e.g., if Gemini provides a textual explanation)
                            return objectMapper.writeValueAsString(Map.of("error", "Failed to parse Gemini API response to JSON: " + e.getMessage() + "\nRaw Response: " + geminiResponse));
                        }
                    }
                }
            }
        }
        return objectMapper.writeValueAsString(Map.of("error", "Error processing PDF or Gemini API response."));
    }
}