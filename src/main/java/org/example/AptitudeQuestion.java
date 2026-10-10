package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AptitudeQuestion {
    public String question;
    public List<String> options;
    public String correctAnswer;
    public String explanation;
    public String quickTrick; // ADD THIS FIELD
    public String difficulty; // Normal, Hard

    // Default constructor for Jackson
    public AptitudeQuestion() {
    }

    // Constructor for testing
    public AptitudeQuestion(String question, List<String> options, String correctAnswer, String explanation) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.quickTrick = "Normal";
    }

    // Constructor with quickTrick
    public AptitudeQuestion(String question, List<String> options, String correctAnswer, String explanation,
            String quickTrick) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.quickTrick = quickTrick;
        this.difficulty = "Normal";
    }

    // Constructor with difficulty
    public AptitudeQuestion(String question, List<String> options, String correctAnswer, String explanation,
            String quickTrick, String difficulty) {
        this.question = question;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.quickTrick = quickTrick;
        this.difficulty = difficulty != null ? difficulty : "Hard";
    }

    @Override
    public String toString() {
        return "AptitudeQuestion{" +
                "question='" + question + '\'' +
                ", options=" + options +
                ", correctAnswer='" + correctAnswer + '\'' +
                ", explanation='" + explanation + '\'' +
                ", quickTrick='" + quickTrick + '\'' +
                ", difficulty='" + difficulty + '\'' +
                '}';
    }
}
