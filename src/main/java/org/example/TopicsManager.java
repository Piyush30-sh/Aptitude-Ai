package org.example;

import java.util.*;

public class TopicsManager {
    private static TopicsManager instance = null;
    private Map<String, List<String>> categories;

    private TopicsManager() {
        loadTopics();
    }

    public static TopicsManager getInstance() {
        if (instance == null) {
            instance = new TopicsManager();
        }
        return instance;
    }

    private void loadTopics() {
        try {
            categories = new LinkedHashMap<>();
            // Load categories and subtopics from SubtopicRepository (single source of
            // truth)
            for (String category : SubtopicRepository.getAllCategories()) {
                List<String> subtopics = SubtopicRepository.getSubtopicsByCategory(category);
                categories.put(category, new ArrayList<>(subtopics));
            }
        } catch (Exception e) {
            System.err.println("Error loading topics: " + e.getMessage());
            categories = new HashMap<>();
        }
    }

    public Set<String> getCategories() {
        return categories.keySet();
    }

    public List<String> getSubtopics(String category) {
        List<String> subtopics = categories.get(category);
        return subtopics != null ? new ArrayList<>(subtopics) : new ArrayList<>();
    }

    public boolean isValidCategory(String category) {
        return categories.containsKey(category);
    }

    public boolean isValidSubtopic(String category, String subtopic) {
        List<String> subtopics = categories.get(category);
        return subtopics != null && subtopics.contains(subtopic);
    }
}
