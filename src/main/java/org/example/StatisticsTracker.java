package org.example;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe statistics tracking with persistence.
 * Handles: topic performance, spaced repetition, patterns, achievements.
 * NO EDT BLOCKING - All I/O on background threads.
 */
public class StatisticsTracker {
    private static final String DATA_DIR = "aptitude_data";
    private static final String STATS_FILE = "statistics.dat";

    // Atomic counters for lock-free, non-blocking updates
    private final AtomicInteger totalAttempted = new AtomicInteger(0);
    private final AtomicInteger totalCorrect = new AtomicInteger(0);
    private final Map<String, TopicStats> topicPerformance = new ConcurrentHashMap<>();

    private final List<QuestionAttempt> attemptHistory = Collections.synchronizedList(new ArrayList<>());

    // Weak area detection
    private final Map<String, WeakAreaMetrics> weakAreas = new ConcurrentHashMap<>();

    // Spaced repetition scheduling
    private final Map<String, ReviewSchedule> reviewSchedules = new ConcurrentHashMap<>();

    // Achievement tracking
    private final Set<String> unlockedAchievements = Collections.synchronizedSet(new HashSet<>());

    private final ScheduledExecutorService ioExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "StatsIO");
        thread.setDaemon(true);
        return thread;
    });
    private final Object persistScheduleLock = new Object();
    private ScheduledFuture<?> pendingPersistTask;

    public StatisticsTracker() {
        ensureDataDir();
        loadStatisticsAsync();
    }

    /**
     * Record a question attempt without blocking EDT.
     * Uses atomic operations + background persistence.
     */
    public void recordAttempt(String topic, String difficulty, String question,
            String selectedAnswer, String correctAnswer,
            long timeSpent, boolean isCorrect) {
        // Non-blocking atomic update
        totalAttempted.incrementAndGet();
        if (isCorrect) {
            totalCorrect.incrementAndGet();
        }

        // Update topic stats
        topicPerformance.computeIfAbsent(topic, k -> new TopicStats(topic))
                .recordAttempt(difficulty, isCorrect, timeSpent);

        // Track weak areas
        if (!isCorrect) {
            weakAreas.computeIfAbsent(topic, k -> new WeakAreaMetrics(topic))
                    .recordMistake(difficulty, question);
        }

        // Store attempt for pattern analysis
        attemptHistory.add(new QuestionAttempt(
                topic, difficulty, question, selectedAnswer, correctAnswer, timeSpent, isCorrect));

        // Schedule review for missed questions
        if (!isCorrect) {
            scheduleReview(topic, question);
        }

        // Check achievements (non-blocking)
        checkAchievements();

        // Persist asynchronously
        persistAsync();
    }

    private void scheduleReview(String topic, String question) {
        String key = topic + "|" + question;
        ReviewSchedule schedule = reviewSchedules.getOrDefault(key, new ReviewSchedule(topic, question));
        schedule.scheduleNextReview(LocalDate.now());
        reviewSchedules.put(key, schedule);
    }

    /**
     * Get topics due for review based on spaced repetition.
     * O(n) but runs on background thread - safe for UI.
     */
    public List<String> getReviewDueTopics() {
        List<String> dueFortopics = new ArrayList<>();
        LocalDate today = LocalDate.now();

        reviewSchedules.forEach((key, schedule) -> {
            if (schedule.isDueForReview(today)) {
                dueFortopics.add(schedule.topic);
            }
        });

        return dueFortopics;
    }

    /**
     * Detect weak areas using statistical analysis.
     * Returns topics with accuracy < 70%.
     */
    public Map<String, Double> getWeakAreas() {
        Map<String, Double> weak = new HashMap<>();
        topicPerformance.forEach((topic, stats) -> {
            double accuracy = stats.getAccuracy();
            if (accuracy < 0.70 && stats.getTotalAttempts() >= 3) {
                weak.put(topic, accuracy);
            }
        });
        return weak;
    }

    /**
     * Pattern recognition: identify common mistakes.
     * Returns map of question patterns → failure count.
     */
    public Map<String, Integer> getCommonMistakePatterns() {
        Map<String, Integer> patterns = new HashMap<>();

        synchronized (attemptHistory) {
            for (QuestionAttempt attempt : attemptHistory) {
                if (!attempt.isCorrect) {
                    // Extract pattern: common keywords in failed questions
                    String pattern = extractKeywords(attempt.question);
                    patterns.merge(pattern, 1, Integer::sum);
                }
            }
        }

        return patterns;
    }

    private String extractKeywords(String question) {
        // Simple keyword extraction - can be enhanced with NLP
        String[] words = question.toLowerCase().split("\\s+");
        StringBuilder keywords = new StringBuilder();

        for (String word : words) {
            if (word.length() > 5 && !isCommonWord(word)) {
                keywords.append(word).append(" ");
                if (keywords.length() > 30)
                    break;
            }
        }

        return keywords.toString().trim();
    }

    private boolean isCommonWord(String word) {
        Set<String> common = Set.of("the", "what", "which", "that", "with", "from", "about");
        return common.contains(word);
    }

    /**
     * Get learning recommendations based on:
     * - Weak areas (low accuracy)
     * - Spaced repetition schedule
     * - Pattern analysis
     */
    public RecommendationResult getLearningRecommendations() {
        RecommendationResult result = new RecommendationResult();

        // Recommendation 1: Review weak areas
        result.weakAreasToReview = new ArrayList<>(getWeakAreas().keySet());

        // Recommendation 2: Topics due for spaced repetition
        result.reviewDueTopics = getReviewDueTopics();

        // Recommendation 3: Common mistake areas
        result.mistakePatterns = getCommonMistakePatterns();

        // Recommendation 4: Suggested next topic (balanced difficulty)
        result.suggestedNextTopic = recommendNextTopic();

        return result;
    }

    private String recommendNextTopic() {
        // Pick a topic with 50-70% accuracy (optimal learning zone)
        return topicPerformance.entrySet().stream()
                .filter(e -> {
                    double acc = e.getValue().getAccuracy();
                    return acc >= 0.5 && acc <= 0.7;
                })
                .max(Comparator.comparingInt(e -> e.getValue().getTotalAttempts()))
                .map(Map.Entry::getKey)
                .orElse("Quantitative");
    }

    private void checkAchievements() {
        int total = totalAttempted.get();
        int correct = totalCorrect.get();

        // Achievement: Speed Demon (5 questions in < 2 minutes)
        if (hasRecentQuestionsInTime(5, 120000)) {
            unlockedAchievements.add("SPEED_DEMON");
        }

        // Achievement: Perfect Streak (5 correct in a row)
        if (hasConsecutiveCorrect(5)) {
            unlockedAchievements.add("PERFECT_STREAK");
        }

        // Achievement: 100% Accuracy on 10 questions
        if (total >= 10 && correct == total) {
            unlockedAchievements.add("FLAWLESS");
        }

        // Achievement: Master Learner (accuracy > 80% on 30 questions)
        if (total >= 30 && correct >= (total * 0.8)) {
            unlockedAchievements.add("MASTER_LEARNER");
        }

        // Achievement: Comeback Kid (missed 5, then got 5 correct)
        if (hasSuccessfulComeback()) {
            unlockedAchievements.add("COMEBACK_KID");
        }
    }

    private boolean hasRecentQuestionsInTime(int count, long timeMs) {
        synchronized (attemptHistory) {
            if (attemptHistory.size() < count)
                return false;

            List<QuestionAttempt> recent = attemptHistory.subList(
                    Math.max(0, attemptHistory.size() - count),
                    attemptHistory.size());

            long firstTime = recent.get(0).timestamp;
            long lastTime = recent.get(recent.size() - 1).timestamp;
            return (lastTime - firstTime) <= timeMs;
        }
    }

    private boolean hasConsecutiveCorrect(int count) {
        synchronized (attemptHistory) {
            if (attemptHistory.size() < count)
                return false;

            int streak = 0;
            for (QuestionAttempt attempt : attemptHistory) {
                if (attempt.isCorrect) {
                    streak++;
                    if (streak >= count)
                        return true;
                } else {
                    streak = 0;
                }
            }

            return false;
        }
    }

    private boolean hasSuccessfulComeback() {
        synchronized (attemptHistory) {
            if (attemptHistory.size() < 10)
                return false;

            int mistakeCount = 0;
            int successCount = 0;
            boolean inMistakePhase = false;

            for (int i = Math.max(0, attemptHistory.size() - 10); i < attemptHistory.size(); i++) {
                QuestionAttempt attempt = attemptHistory.get(i);

                if (!attempt.isCorrect) {
                    mistakeCount++;
                    inMistakePhase = true;
                    successCount = 0;
                } else {
                    if (inMistakePhase) {
                        successCount++;
                    }
                }
            }

            return mistakeCount >= 5 && successCount >= 5;
        }
    }

    public int getTotalAttempted() {
        return totalAttempted.get();
    }

    public int getTotalCorrect() {
        return totalCorrect.get();
    }

    public double getOverallAccuracy() {
        int total = totalAttempted.get();
        return total == 0 ? 0 : (double) totalCorrect.get() / total * 100;
    }

    public Map<String, TopicStats> getAllTopicStats() {
        return new HashMap<>(topicPerformance);
    }

    public Set<String> getUnlockedAchievements() {
        return new HashSet<>(unlockedAchievements);
    }

    /**
     * Returns total recorded study time across all attempts in milliseconds.
     */
    public long getTotalStudyTimeMillis() {
        long total = 0L;
        synchronized (attemptHistory) {
            for (QuestionAttempt attempt : attemptHistory) {
                total += Math.max(0L, attempt.timeSpent);
            }
        }
        return total;
    }

    /**
     * Returns average response time in seconds.
     */
    public double getAverageTimeSeconds() {
        synchronized (attemptHistory) {
            if (attemptHistory.isEmpty()) {
                return 0.0;
            }

            long total = 0L;
            for (QuestionAttempt attempt : attemptHistory) {
                total += Math.max(0L, attempt.timeSpent);
            }
            return (total / 1000.0) / attemptHistory.size();
        }
    }

    /**
     * Returns best historical consecutive correct streak.
     */
    public int getBestStreak() {
        int best = 0;
        int current = 0;

        synchronized (attemptHistory) {
            for (QuestionAttempt attempt : attemptHistory) {
                if (attempt.isCorrect) {
                    current++;
                    best = Math.max(best, current);
                } else {
                    current = 0;
                }
            }
        }

        return best;
    }

    /**
     * Returns number of unique topics attempted so far.
     */
    public int getUniqueTopicCount() {
        return topicPerformance.size();
    }

    /**
     * Returns an accuracy trend snapshot as percentage points.
     * Data is bucketed from oldest to newest based on recent history.
     */
    public List<Integer> getRecentAccuracyTrend(int points) {
        int safePoints = Math.max(2, Math.min(points, 20));
        List<Integer> trend = new ArrayList<>();

        synchronized (attemptHistory) {
            if (attemptHistory.isEmpty()) {
                return trend;
            }

            int size = attemptHistory.size();
            int bucketSize = Math.max(1, size / safePoints);

            for (int start = 0; start < size; start += bucketSize) {
                int end = Math.min(size, start + bucketSize);
                int total = 0;
                int correct = 0;

                for (int i = start; i < end; i++) {
                    QuestionAttempt attempt = attemptHistory.get(i);
                    total++;
                    if (attempt.isCorrect) {
                        correct++;
                    }
                }

                int pct = total == 0 ? 0 : (int) Math.round((correct * 100.0) / total);
                trend.add(pct);
            }
        }

        if (trend.size() > safePoints) {
            trend = new ArrayList<>(trend.subList(trend.size() - safePoints, trend.size()));
        }
        if (trend.size() == 1) {
            trend.add(trend.get(0));
        }
        return trend;
    }

    // ==================== PERSISTENCE ====================

    private void ensureDataDir() {
        File dir = AppPaths.projectFile(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void persistAsync() {
        synchronized (persistScheduleLock) {
            if (pendingPersistTask != null && !pendingPersistTask.isDone()) {
                return;
            }
            pendingPersistTask = ioExecutor.schedule(this::persist, 300, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void persist() {
        try {
            File file = new File(AppPaths.projectFile(DATA_DIR), STATS_FILE);
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(new StatisticsSnapshot(
                        topicPerformance,
                        attemptHistory,
                        weakAreas,
                        reviewSchedules,
                        unlockedAchievements,
                        totalAttempted.get(),
                        totalCorrect.get()));
            }
        } catch (Exception e) {
            System.err.println("Failed to persist statistics: " + e.getMessage());
        }
    }

    private void loadStatisticsAsync() {
        ioExecutor.execute(this::loadStatistics);
    }

    private synchronized void loadStatistics() {
        try {
            File file = new File(AppPaths.projectFile(DATA_DIR), STATS_FILE);
            if (!file.exists())
                return;

            StatisticsSnapshot snapshot;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                snapshot = (StatisticsSnapshot) ois.readObject();
            }

            topicPerformance.putAll(snapshot.topicPerformance);
            attemptHistory.addAll(snapshot.attemptHistory);
            weakAreas.putAll(snapshot.weakAreas);
            reviewSchedules.putAll(snapshot.reviewSchedules);
            unlockedAchievements.addAll(snapshot.unlockedAchievements);
            totalAttempted.set(snapshot.totalAttempted);
            totalCorrect.set(snapshot.totalCorrect);
        } catch (Exception e) {
            System.err.println("Failed to load statistics: " + e.getMessage());
        }
    }

    // ==================== DATA CLASSES ====================

    public static class TopicStats implements Serializable {
        private static final long serialVersionUID = 1L;
        public String topic;
        public int normalCorrect, normalTotal, hardCorrect, hardTotal;
        public long totalTimeSpent;

        public TopicStats(String topic) {
            this.topic = topic;
        }

        public void recordAttempt(String difficulty, boolean isCorrect, long timeSpent) {
            totalTimeSpent += timeSpent;

            if ("Hard".equals(difficulty)) {
                hardTotal++;
                if (isCorrect)
                    hardCorrect++;
            } else {
                // Treat any non-Hard difficulty (Normal, Easy, Medium) as Normal
                normalTotal++;
                if (isCorrect)
                    normalCorrect++;
            }
        }

        public double getAccuracy() {
            int total = getTotalAttempts();
            if (total == 0)
                return 0;
            return (double) (normalCorrect + hardCorrect) / total;
        }

        public int getTotalAttempts() {
            return normalTotal + hardTotal;
        }
    }

    public static class WeakAreaMetrics implements Serializable {
        private static final long serialVersionUID = 1L;
        public String topic;
        public List<String> failedQuestions = new ArrayList<>();
        public Map<String, Integer> difficultyMistakes = new ConcurrentHashMap<>();

        public WeakAreaMetrics(String topic) {
            this.topic = topic;
        }

        public void recordMistake(String difficulty, String question) {
            failedQuestions.add(question);
            difficultyMistakes.merge(difficulty, 1, Integer::sum);
        }
    }

    public static class ReviewSchedule implements Serializable {
        public String topic;
        public String question;
        public List<LocalDate> reviewDates = new ArrayList<>();

        public ReviewSchedule(String topic, String question) {
            this.topic = topic;
            this.question = question;
        }

        public void scheduleNextReview(LocalDate baseDate) {
            // Spaced repetition: 1 day, 3 days, 7 days, 14 days
            int reviewCount = reviewDates.size();
            int daysUntilNext = (int) Math.pow(2, reviewCount);
            reviewDates.add(baseDate.plusDays(daysUntilNext));
        }

        public boolean isDueForReview(LocalDate today) {
            if (reviewDates.isEmpty())
                return true;
            return !today.isBefore(reviewDates.get(reviewDates.size() - 1));
        }
    }

    public static class QuestionAttempt implements Serializable {
        public String topic, difficulty, question, selectedAnswer, correctAnswer;
        public long timeSpent;
        public boolean isCorrect;
        public long timestamp;

        public QuestionAttempt(String topic, String difficulty, String question,
                String selectedAnswer, String correctAnswer,
                long timeSpent, boolean isCorrect) {
            this.topic = topic;
            this.difficulty = difficulty;
            this.question = question;
            this.selectedAnswer = selectedAnswer;
            this.correctAnswer = correctAnswer;
            this.timeSpent = timeSpent;
            this.isCorrect = isCorrect;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public static class RecommendationResult {
        public List<String> weakAreasToReview;
        public List<String> reviewDueTopics;
        public Map<String, Integer> mistakePatterns;
        public String suggestedNextTopic;
    }

    private static class StatisticsSnapshot implements Serializable {
        Map<String, TopicStats> topicPerformance;
        List<QuestionAttempt> attemptHistory;
        Map<String, WeakAreaMetrics> weakAreas;
        Map<String, ReviewSchedule> reviewSchedules;
        Set<String> unlockedAchievements;
        int totalAttempted, totalCorrect;

        StatisticsSnapshot(Map<String, TopicStats> topicPerformance,
                List<QuestionAttempt> attemptHistory,
                Map<String, WeakAreaMetrics> weakAreas,
                Map<String, ReviewSchedule> reviewSchedules,
                Set<String> unlockedAchievements,
                int totalAttempted, int totalCorrect) {
            this.topicPerformance = topicPerformance;
            this.attemptHistory = attemptHistory;
            this.weakAreas = weakAreas;
            this.reviewSchedules = reviewSchedules;
            this.unlockedAchievements = unlockedAchievements;
            this.totalAttempted = totalAttempted;
            this.totalCorrect = totalCorrect;
        }
    }
}
