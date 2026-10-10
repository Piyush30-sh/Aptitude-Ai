package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * ManualQuestionsRepository: Fallback questions when API token is exceeded.
 * Loads manually created JSON files from src/main/resources/manual_questions/
 */
public class ManualQuestionsRepository {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Random rand = new Random();

    /**
     * Load a random question from the manual questions file for a subtopic
     * Returns null if file doesn't exist or is empty
     */
    public static AptitudeQuestion getRandomManualQuestion(String subtopic, String difficulty) {
        try {
            // Normalize subtopic name to filename (spaces to underscores)
            String filename = subtopic.replace(" ", "_") + ".json";
            String resourcePath = "/manual_questions/" + filename;

            // Load JSON file from resources
            InputStream inputStream = ManualQuestionsRepository.class.getResourceAsStream(resourcePath);
            if (inputStream == null) {
                System.err.println("⚠️  No manual questions file found for: " + subtopic);
                return null;
            }

            // Parse JSON
            JsonNode root = mapper.readTree(inputStream);
            JsonNode questionsArray = root.path("questions");

            if (!questionsArray.isArray() || questionsArray.size() == 0) {
                System.err.println("⚠️  No questions in manual file for: " + subtopic);
                return null;
            }

            // Pick random question
            int randomIndex = rand.nextInt(questionsArray.size());
            JsonNode questionNode = questionsArray.get(randomIndex);

            // Convert to AptitudeQuestion
            AptitudeQuestion q = new AptitudeQuestion();
            q.question = questionNode.path("question").asText();
            q.correctAnswer = questionNode.path("correctAnswer").asText();
            q.explanation = questionNode.path("explanation").asText();
            q.difficulty = (difficulty == null || difficulty.isEmpty()) ? "Normal" : difficulty;

            // Parse options as List<String>
            JsonNode optionsArray = questionNode.path("options");
            List<String> optionsList = new ArrayList<>();
            if (optionsArray.isArray() && optionsArray.size() == 4) {
                for (int i = 0; i < 4; i++) {
                    optionsList.add(optionsArray.get(i).asText());
                }
                q.options = optionsList;
            }

            // No quickTrick in manual questions
            q.quickTrick = null;

            System.err.println("✅ Loaded manual question from: " + subtopic);
            return q;

        } catch (Exception e) {
            System.err.println("❌ Error loading manual question: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get all available manual question subtopics
     */
    public static List<String> getAvailableSubtopics() {
        List<String> available = new ArrayList<>();
        String[] subtopics = {
                "Arithmetic", "Time_and_Work", "Time_Speed_and_Distance", "Number_System",
                "Puzzles", "Coding_Decoding", "Blood_Relation", "Syllogism",
                "Grammar", "Vocabulary", "Reading_Comprehension", "Data_Interpretation"
        };

        for (String subtopic : subtopics) {
            InputStream is = ManualQuestionsRepository.class
                    .getResourceAsStream("/manual_questions/" + subtopic + ".json");
            if (is != null) {
                available.add(subtopic.replace("_", " "));
                try {
                    is.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
        return available;
    }
}
