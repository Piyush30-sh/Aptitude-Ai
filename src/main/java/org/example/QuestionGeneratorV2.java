package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class QuestionGeneratorV2 {
        private final HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
        private final ObjectMapper mapper = new ObjectMapper();
        private final String[] preferredModels = { "llama-3.3-70b-versatile" };

        public AptitudeQuestion generateNewQuestion(String topic, String subtopic, String difficulty) {
                if (subtopic == null || !SubtopicRepository.isValidSubtopic(subtopic)) {
                        System.err.println("❌ Invalid subtopic: " + subtopic);
                        return null;
                }

                String d = (difficulty == null) ? "Normal"
                                : (difficulty.toLowerCase().startsWith("h") ? "Hard" : "Normal");

                SubtopicRepository.SubtopicInfo info = SubtopicRepository.getSubtopicInfo(subtopic);
                if (info == null) {
                        System.err.println("❌ No info for subtopic: " + subtopic);
                        return null;
                }

                String prompt = d.equals("Hard") ? info.promptHard : info.promptNormal;
                String key = System.getenv("GROQ_API_KEY");
                if (key == null || key.trim().isEmpty()) {
                        System.err.println("❌ GROQ_API_KEY environment variable not set!");
                        System.err.println("   Get free key from: https://console.groq.com/keys");
                        return null;
                }

                for (String m : resolveModels(key)) {
                        try {
                                // Build proper JSON request body
                                ObjectNode requestBody = mapper.createObjectNode()
                                                .put("model", m)
                                                .put("temperature", 0.7)
                                                .put("max_tokens", 500);

                                requestBody.set("messages", mapper.createArrayNode()
                                                .add(mapper.createObjectNode()
                                                                .put("role", "system")
                                                                .put("content", "Generate a single JSON object with: question, options (array of 4), correctAnswer."))
                                                .add(mapper.createObjectNode()
                                                                .put("role", "user")
                                                                .put("content", prompt)));

                                String jsonBody = requestBody.toString();
                                System.err.println("📤 Calling Groq API with model: " + m);

                                HttpRequest req = HttpRequest.newBuilder()
                                                .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                                                .header("Authorization", "Bearer " + key.trim())
                                                .header("Content-Type", "application/json")
                                                .timeout(Duration.ofSeconds(30))
                                                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                                                .build();

                                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                                System.err.println("📥 Response status: " + resp.statusCode());

                                if (resp.statusCode() != 200) {
                                        System.err.println("❌ API Error " + resp.statusCode() + ": " + resp.body());
                                        continue;
                                }

                                String respBody = resp.body();
                                JsonNode root = mapper.readTree(respBody);
                                String content = root.path("choices").get(0).path("message").path("content").asText();
                                System.err.println("📄 Content: "
                                                + content.substring(0, Math.min(100, content.length())) + "...");

                                String json = extractJson(content);
                                if (json == null) {
                                        System.err.println("❌ Could not extract JSON from response");
                                        continue;
                                }

                                AptitudeQuestion q = mapper.readValue(json, AptitudeQuestion.class);
                                if (!isValid(q)) {
                                        System.err.println("❌ Question failed validation");
                                        continue;
                                }

                                q.difficulty = d;
                                System.err.println("✅ Question generated successfully!");
                                return q;

                        } catch (InterruptedException e) {
                                System.err.println("⚠️ Request interrupted: " + e.getMessage());
                                Thread.currentThread().interrupt();
                                continue;
                        } catch (Exception e) {
                                System.err.println("❌ Exception with model " + m + ": " + e.getMessage());
                                e.printStackTrace();
                        }
                }

                System.err.println("❌ All models failed. Check API key and network.");
                System.err.println("📋 Attempting to load manual questions as fallback...");

                // Fallback to manual questions when API fails
                AptitudeQuestion fallback = ManualQuestionsRepository.getRandomManualQuestion(subtopic, d);
                if (fallback != null) {
                        System.err.println("✅ Loaded fallback question from manual repository");
                        return fallback;
                }

                System.err.println("❌ No fallback questions available.");
                return null;
        }

        private boolean isValid(AptitudeQuestion q) {
                if (q == null || q.question == null || q.options == null || q.correctAnswer == null) {
                        System.err.println("❌ Question or options is null");
                        return false;
                }
                if (q.options.size() != 4) {
                        System.err.println("❌ Expected 4 options, got " + q.options.size());
                        return false;
                }

                java.util.HashSet<String> distinct = new java.util.HashSet<>();
                for (String o : q.options) {
                        if (o == null || o.trim().isEmpty()) {
                                System.err.println("❌ Null or empty option");
                                return false;
                        }
                        distinct.add(o.toLowerCase().trim());
                }

                if (distinct.size() != 4) {
                        System.err.println("❌ Options not distinct (got " + distinct.size() + " unique)");
                        return false;
                }

                boolean correctFound = false;
                for (String o : q.options) {
                        if (q.correctAnswer.trim().equalsIgnoreCase(o.trim())) {
                                correctFound = true;
                                break;
                        }
                }
                if (!correctFound) {
                        System.err.println("❌ Correct answer not in options");
                        return false;
                }

                System.err.println("✅ Validation passed");
                return true;
        }

        private String extractJson(String text) {
                if (text == null || text.isEmpty()) {
                        return null;
                }

                int start = text.indexOf('{');
                int end = text.lastIndexOf('}');

                if (start < 0 || end <= start) {
                        System.err.println(
                                        "❌ No JSON braces found in: " + text.substring(0, Math.min(50, text.length())));
                        return null;
                }

                return text.substring(start, end + 1).trim();
        }

        private String[] resolveModels(String k) {
                return preferredModels;
        }
}
