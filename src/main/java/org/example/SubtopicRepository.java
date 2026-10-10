package org.example;

import java.util.*;

/**
 * SubtopicRepository: Complete information and guidelines for all 11 subtopics
 * Provides 100% precise, current (2026) question generation templates
 * Independent from other generators - contains all necessary context
 */
public class SubtopicRepository {

        // Arithmetic: Basic calculations, word problems with Indian context
        public static final String ARITHMETIC_DEFINITION = "Arithmetic problems involving addition, subtraction, multiplication, division with real-world scenarios. "
                        +
                        "Context: Shopping, salary, distance, age, volume. " +
                        "Approach: 2-step calculation for Normal, 3-4 steps for Hard. " +
                        "2026 Trends: Banking calculations, GST, currency conversions, profit margins.";

        public static final String ARITHMETIC_PROMPT_NORMAL = "Generate an arithmetic word problem with real-world Indian context (shopping, salary, construction, agriculture). "
                        +
                        "Include exact numeric values. Problem should require 2 calculation steps maximum. " +
                        "Ask for ONE unknown value. " +
                        "Provide 4 distinct options where exactly one is correct. Other three must be from common calculation errors: "
                        +
                        "1) Wrong operation (+ instead of -, × instead of ÷). 2) Decimal/rounding error. 3) Forgetting a step. 4) Unit conversion mistake.";

        public static final String ARITHMETIC_PROMPT_HARD = "Generate a complex arithmetic word problem requiring 3-4 calculation steps with real-world Indian context. "
                        +
                        "Include nested operations and real values (prices, percentages, quantities). " +
                        "Example: 'A shopkeeper buys 100 items at Rs.50 each, marks up 30%, gives 10% discount. Calculate final profit.' "
                        +
                        "Provide 4 options: one correct (after all calculations), three from common mistakes at different steps.";

        // Time and Work: Worker productivity, combined work rates
        public static final String TIME_AND_WORK_DEFINITION = "Problems involving work rates, time to complete tasks, combined work by multiple workers/machines. "
                        +
                        "Context: Construction, manufacturing, software development, agriculture. " +
                        "Concept: Work = Rate × Time, Combined rates = sum of individual rates. " +
                        "2026 Trends: Automation scenarios, factory shifts, project deadlines.";

        public static final String TIME_AND_WORK_PROMPT_NORMAL = "Generate a time-and-work problem with 2 workers/machines with clearly stated individual work rates or times to complete a task. "
                        +
                        "Ask: 'If both work together, how long to finish?' or 'If A works for 2 days, then B joins, how many more days needed?' "
                        +
                        "Provide 4 options: correct answer (using rate addition), three from mistakes like: " +
                        "1) Summing times directly. 2) Wrong rate calculation. 3) Forgetting partial work.";

        public static final String TIME_AND_WORK_PROMPT_HARD = "Generate a time-and-work problem with 3 workers with different rates, involving work done in phases. "
                        +
                        "Example: 'A does 1/4 work in 5 days, B does remaining in 8 days, if C joins after 3 days completing task together in X days, find X.' "
                        +
                        "Provide 4 options with intricate fraction calculations in correct answer, three from errors in partial work tracking.";

        // Time Speed Distance: Motion problems with Indian routes
        public static final String TIME_SPEED_DISTANCE_DEFINITION = "Problems about motion (trains, vehicles, boats) with distance, speed, and time relationships. "
                        +
                        "Context: Indian railway routes (Delhi-Mumbai 1400km), road travel, boat racing. " +
                        "Concept: Speed = Distance/Time, Relative speed for same/opposite direction. " +
                        "2026 Trends: Electric vehicles, high-speed rail, real-time traffic scenarios.";

        public static final String TIME_SPEED_DISTANCE_PROMPT_NORMAL = "Generate a speed-time-distance problem with a train or vehicle traveling between two Indian cities with clear distance (km) and speed (km/h). "
                        +
                        "Ask for time taken, distance, or speed. Example: 'Train travels Delhi to Agra (240km) at 60km/h. Time taken?' "
                        +
                        "Provide 4 options: correct (240/60=4 hours), three from errors: 1) Speed×Time. 2) Wrong unit conversion. 3) Average vs constant speed confusion.";

        public static final String TIME_SPEED_DISTANCE_PROMPT_HARD = "Generate a complex problem with two vehicles/trains with different speeds, relative motion, or meeting scenarios. "
                        +
                        "Example: 'Train A leaves Delhi at 60km/h, Train B leaves 2 hours later at 80km/h. When do they meet? (Assume 500km total)' "
                        +
                        "Provide 4 options with correct answer from relative speed equation, three from errors in time-offset calculations.";

        // Number System: Divisibility, primes, HCF/LCM, digit properties
        public static final String NUMBER_SYSTEM_DEFINITION = "Problems involving natural numbers, divisibility rules, prime numbers, HCF (GCD), LCM, digit sums, number properties. "
                        +
                        "Concept: Divisibility by 2,3,5,9,11; Prime factorization; HCF×LCM=A×B. " +
                        "Context: Finding common factors, reducing fractions, scheduling. " +
                        "2026 Trends: Cryptography basics, digital security, number patterns.";

        public static final String NUMBER_SYSTEM_PROMPT_NORMAL = "Generate a number system problem about HCF, LCM, or divisibility. "
                        +
                        "Ask: 'Find HCF of 48 and 64' or 'What is the smallest number divisible by 6, 8, 12?' or 'How many numbers from 1-100 are divisible by 3 but not 5?' "
                        +
                        "Provide 4 options: one correct answer, three from: 1) Wrong divisibility rule. 2) Confusing HCF/LCM. 3) Missing edge cases.";

        public static final String NUMBER_SYSTEM_PROMPT_HARD = "Generate a complex number system problem combining multiple concepts. "
                        +
                        "Example: 'Find the greatest number that divides 156, 247, 338 leaving remainders 12, 7, 14 respectively.' "
                        +
                        "Provide 4 options requiring deep understanding of divisibility and remainder theorem.";

        // Logical Reasoning: Puzzles - visual, spatial, logic grids
        public static final String PUZZLES_DEFINITION = "Logic puzzles, pattern recognition, spatial reasoning, and analytical thinking problems. "
                        +
                        "Types: Sequence patterns, visual puzzles, logic grids, matchstick problems. " +
                        "Context: Analytical decision-making, strategic thinking. " +
                        "2026 Trends: AI pattern recognition, game theory, optimization puzzles.";

        public static final String PUZZLES_PROMPT_NORMAL = "Generate a logic puzzle or pattern recognition problem. " +
                        "Types: 1) Figure series (shapes/patterns - find next). 2) Number sequence (1, 4, 9, 16, ? find next). 3) Matchstick: rearrange N sticks to form X shapes. "
                        +
                        "Provide 4 visual/numeric options where one correctly completes the pattern. Make distractors plausible but wrong.";

        public static final String PUZZLES_PROMPT_HARD = "Generate a complex logic puzzle requiring multi-step reasoning. "
                        +
                        "Example: 'Five people sit in a row. A is 3 positions right of B. C is not adjacent to D. E is at end. Find positions.' "
                        +
                        "Provide 4 arrangement options where only one satisfies all constraints.";

        // Coding Decoding: Letter/number substitution, cipher patterns
        public static final String CODING_DECODING_DEFINITION = "Problems involving letter-to-number conversions, cipher patterns, code-breaking. "
                        +
                        "Types: Letter shift (A=1, Z=26), word coding, pattern substitution. " +
                        "Context: Message encryption, pattern matching. " +
                        "2026 Trends: Cybersecurity, encoding algorithms, data compression.";

        public static final String CODING_DECODING_PROMPT_NORMAL = "Generate a coding-decoding problem with simple substitution or positional shift. "
                        +
                        "Example: 'If MOTHER is coded as 13-15-20-8-5-18, code SISTER.' or 'Each letter moves 2 positions forward: A→C, BALL→DCKN. Code TABLE.' "
                        +
                        "Provide 4 coded options where one is correct and three from: 1) Reverse shift. 2) Wrong letter mapping. 3) Partial application.";

        public static final String CODING_DECODING_PROMPT_HARD = "Generate a complex coding problem with double encoding or pattern-based substitution. "
                        +
                        "Example: 'Vowels are reversed (A↔E, O↔U) AND consonants shift by 2. HOUSE becomes ?. RAINBOW becomes ?' "
                        +
                        "Provide 4 options testing multi-step encoding understanding.";

        // Blood Relation: Family tree, relationship deduction
        public static final String BLOOD_RELATION_DEFINITION = "Problems determining relationships between family members through given clues. "
                        +
                        "Relationships: Parent-child, sibling, spouse, grandparent, cousin, uncle/aunt. " +
                        "Method: Draw family tree, trace lineage, use kinship logic. " +
                        "2026 Trends: Complex modern families, inheritance scenarios.";

        public static final String BLOOD_RELATION_PROMPT_NORMAL = "Generate a blood relation problem with 4-5 clear relationship statements. "
                        +
                        "Example: 'A is the father of B. C is the sister of B. D is the son of C. How is D related to A?' "
                        +
                        "Answer: Grandson/Granddaughter. " +
                        "Provide 4 options: one correct (cousin/nephew/niece/etc), three from common confusion (sibling vs cousin, parent vs uncle, etc).";

        public static final String BLOOD_RELATION_PROMPT_HARD = "Generate a complex family tree with 6-7 people, gender ambiguity clues, and multiple relationship deductions. "
                        +
                        "Example: 'X is Y's mother. Z is Y's son. W is Z's father. M is the father of X's brother. How are M and Z related?' "
                        +
                        "Provide 4 options requiring careful family tree navigation.";

        // Syllogism: Logical deduction from premises
        public static final String SYLLOGISM_DEFINITION = "Deductive reasoning from premises to conclusions using logical statements. "
                        +
                        "Format: All A are B. Some B are C. Therefore, what can we conclude about A and C? " +
                        "Logic: Valid deductions vs invalid assumptions. " +
                        "2026 Trends: AI logic reasoning, automated decision systems.";

        public static final String SYLLOGISM_PROMPT_NORMAL = "Generate a two-statement syllogism problem. " +
                        "Example: 'All birds are animals. Crows are birds. Conclusions: (A) All animals are birds (False). (B) All crows are animals (True). (C) Some animals are crows (True). (D) No birds are animals (False). Which is definitely true?' "
                        +
                        "Provide 4 options: one correct conclusion, three from logical fallacies.";

        public static final String SYLLOGISM_PROMPT_HARD = "Generate a three-statement syllogism with 'Some' and 'All' mixed statements. "
                        +
                        "Test: All As are Bs. Some Bs are Cs. No Cs are Ds. What's definitely/possibly true? " +
                        "Provide 4 conclusion options testing understanding of necessary vs possible truths.";

        // Grammar: Tenses, sentence structure, error detection
        public static final String GRAMMAR_DEFINITION = "English grammar problems: tenses, subject-verb agreement, article usage, prepositions, sentence structure. "
                        +
                        "Focus: Common errors in business/academic English. " +
                        "Context: Proofreading, professional communication. " +
                        "2026 Trends: AI text generation, automated grammar checking.";

        public static final String GRAMMAR_PROMPT_NORMAL = "Generate a grammar error detection problem with 4 sentences, one/two containing errors. "
                        +
                        "Error types: Tense mismatch ('He go to school'), agreement ('The team are winning'), article misuse ('I saw an apple'→'a'). "
                        +
                        "Ask: 'Which sentence is grammatically correct?' or 'Find the error in sentence B.' " +
                        "Provide 4 options with clear correct answer.";

        public static final String GRAMMAR_PROMPT_HARD = "Generate a complex grammar problem with subtle errors in reported speech, conditional tenses, or complex sentence structure. "
                        +
                        "Example: 'If I would have known, I would have helped' (should be 'had known'). " +
                        "Provide 4 options testing nuanced grammar rules.";

        // Vocabulary: Word meanings, synonyms, antonyms
        public static final String VOCABULARY_DEFINITION = "Word meanings, synonyms, antonyms, word usage in context. "
                        +
                        "Focus: Professional/academic vocabulary, contextual word meaning. " +
                        "Context: Reading comprehension support, expression precision. " +
                        "2026 Trends: Business English, specialized terminology.";

        public static final String VOCABULARY_PROMPT_NORMAL = "Generate a vocabulary problem: 'The word PRAGMATIC most closely means: (A) Impractical (B) Practical (C) Ancient (D) Powerful.' "
                        +
                        "Or synonym/antonym: 'EPHEMERAL is opposite of: (A) Permanent (B) Temporary (C) Quick (D) Slow.' "
                        +
                        "Provide 4 clear options with one correct answer.";

        public static final String VOCABULARY_PROMPT_HARD = "Generate a contextual vocabulary problem where word meaning depends on usage context. "
                        +
                        "Example: 'His NOVEL approach surprised everyone' vs 'He wrote a NOVEL.' Same word, different meaning. "
                        +
                        "Ask to identify correct meaning from context. Provide 4 plausible meaning options.";

        // Reading Comprehension: Passage analysis, inference, details
        public static final String READING_COMPREHENSION_DEFINITION = "Comprehension of written passages, detail extraction, inference, main idea identification. "
                        +
                        "Passage length: 150-250 words. Types: Factual, analytical, opinion-based. " +
                        "Question types: Literal (stated), inferential (implied), main idea. " +
                        "2026 Trends: Digital literacy, fake news detection, source evaluation.";

        public static final String READING_COMPREHENSION_PROMPT_NORMAL = "Generate a short reading passage (150-200 words) on a current topic (technology, environment, economics) followed by a question. "
                        +
                        "Question type: Literal (answer directly from passage) or simple inference. " +
                        "Example passage: 'Renewable energy sources like solar and wind are becoming increasingly cost-effective. India aims to generate 500GW from renewables by 2030...' "
                        +
                        "Question: 'What is India's renewable energy target by 2030?' Answer in passage: 500GW. " +
                        "Provide 4 options: one from passage, three plausible-sounding wrong answers.";

        public static final String READING_COMPREHENSION_PROMPT_HARD = "Generate a longer passage (200-250 words) with nuanced argument, followed by inference/analysis question. "
                        +
                        "Question type: 'The author implies that...' or 'What is the main purpose of the passage?' " +
                        "Require combining multiple sentences for answer. Provide 4 options with one correct inference, three from partial reading/misinterpretation.";

        // Profit and Loss: Cost, selling price, profit/loss percentage, markup
        public static final String PROFIT_AND_LOSS_DEFINITION = "Problems involving cost price, selling price, profit, loss, and percentage calculations. "
                        +
                        "Context: Business transactions, merchant calculations, retail pricing, wholesale vs retail. "
                        +
                        "Concept: Profit = SP - CP, Profit% = (Profit/CP)×100, Markup vs Margin. "
                        +
                        "2026 Trends: E-commerce pricing, dynamic pricing, supply chain costs.";

        public static final String PROFIT_AND_LOSS_PROMPT_NORMAL = "Generate a profit and loss problem with clear cost price and selling price. "
                        +
                        "Ask for profit amount, profit percentage, or selling price. "
                        +
                        "Example: 'A shopkeeper bought 50 shirts at Rs.200 each and sold at Rs.250 each. Calculate profit percentage.' "
                        +
                        "Provide 4 options: one correct answer (from (250-200)/200×100 = 25%), three from: "
                        +
                        "1) Using SP instead of CP for denominator. 2) Forgetting to multiply by 100. 3) Calculating profit amount instead of percentage.";

        public static final String PROFIT_AND_LOSS_PROMPT_HARD = "Generate a complex profit and loss problem with multiple items, discounts, or markup scenarios. "
                        +
                        "Example: 'A merchant buys 100 items at Rs.400 each, marks up 40%, gives 15% discount, sells. If overhead is Rs.5000, find profit percent.' "
                        +
                        "Provide 4 options requiring multi-step calculations: markup application, discount application, overhead deduction.";

        // Data Interpretation: Tables, graphs, calculations
        public static final String DATA_INTERPRETATION_DEFINITION = "Analyzing data from tables, bar/pie charts, line graphs, case lets. "
                        +
                        "Skills: Data extraction, percentage calculation, comparison, growth rate analysis. " +
                        "Context: Business analytics, survey results, financial reports. " +
                        "2026 Trends: Big data analysis, real-time dashboards, predictive analytics.";

        public static final String DATA_INTERPRETATION_PROMPT_NORMAL = "Generate a data interpretation problem with a simple table or single chart. "
                        +
                        "Example: 'Table shows sales by region Q1-Q4. Q1: North=100, South=80, East=120. Q2: North=110, South=85, East=125. "
                        +
                        "Which region had highest growth Q1→Q2?' Answer: East (125-120=5) is highest absolute, or calculate percentage. "
                        +
                        "Provide 4 options: one correct calculation, three from: 1) Wrong region. 2) Confused absolute vs percentage. 3) Wrong quarter.";

        public static final String DATA_INTERPRETATION_PROMPT_HARD = "Generate a complex data interpretation with multiple charts or a case let requiring multi-step calculations. "
                        +
                        "Example: 'Given table of company revenue, expenses, and profit margins. Also pie chart of expense breakdown. "
                        +
                        "Calculate: If labor costs (35% of expenses) increase by 20%, what is new profit margin?' " +
                        "Provide 4 options requiring accurate data extraction and nested calculations.";

        // Repository of all subtopics
        private static final Map<String, SubtopicInfo> SUBTOPIC_REPO = new LinkedHashMap<String, SubtopicInfo>() {
                {
                        put("Arithmetic", new SubtopicInfo("Arithmetic", "Quantitative Aptitude", ARITHMETIC_DEFINITION,
                                        ARITHMETIC_PROMPT_NORMAL, ARITHMETIC_PROMPT_HARD));
                        put("Time and Work",
                                        new SubtopicInfo("Time and Work", "Quantitative Aptitude",
                                                        TIME_AND_WORK_DEFINITION,
                                                        TIME_AND_WORK_PROMPT_NORMAL, TIME_AND_WORK_PROMPT_HARD));
                        put("Time Speed and Distance",
                                        new SubtopicInfo("Time Speed and Distance", "Quantitative Aptitude",
                                                        TIME_SPEED_DISTANCE_DEFINITION,
                                                        TIME_SPEED_DISTANCE_PROMPT_NORMAL,
                                                        TIME_SPEED_DISTANCE_PROMPT_HARD));
                        put("Number System",
                                        new SubtopicInfo("Number System", "Quantitative Aptitude",
                                                        NUMBER_SYSTEM_DEFINITION,
                                                        NUMBER_SYSTEM_PROMPT_NORMAL, NUMBER_SYSTEM_PROMPT_HARD));
                        put("Profit and Loss",
                                        new SubtopicInfo("Profit and Loss", "Quantitative Aptitude",
                                                        PROFIT_AND_LOSS_DEFINITION,
                                                        PROFIT_AND_LOSS_PROMPT_NORMAL, PROFIT_AND_LOSS_PROMPT_HARD));
                        put("Puzzles", new SubtopicInfo("Puzzles", "Logical Reasoning", PUZZLES_DEFINITION,
                                        PUZZLES_PROMPT_NORMAL,
                                        PUZZLES_PROMPT_HARD));
                        put("Coding Decoding",
                                        new SubtopicInfo("Coding Decoding", "Logical Reasoning",
                                                        CODING_DECODING_DEFINITION,
                                                        CODING_DECODING_PROMPT_NORMAL, CODING_DECODING_PROMPT_HARD));
                        put("Blood Relation",
                                        new SubtopicInfo("Blood Relation", "Logical Reasoning",
                                                        BLOOD_RELATION_DEFINITION,
                                                        BLOOD_RELATION_PROMPT_NORMAL, BLOOD_RELATION_PROMPT_HARD));
                        put("Syllogism", new SubtopicInfo("Syllogism", "Logical Reasoning", SYLLOGISM_DEFINITION,
                                        SYLLOGISM_PROMPT_NORMAL, SYLLOGISM_PROMPT_HARD));
                        put("Grammar", new SubtopicInfo("Grammar", "Verbal Ability", GRAMMAR_DEFINITION,
                                        GRAMMAR_PROMPT_NORMAL,
                                        GRAMMAR_PROMPT_HARD));
                        put("Vocabulary", new SubtopicInfo("Vocabulary", "Verbal Ability", VOCABULARY_DEFINITION,
                                        VOCABULARY_PROMPT_NORMAL, VOCABULARY_PROMPT_HARD));
                        put("Reading Comprehension",
                                        new SubtopicInfo("Reading Comprehension", "Verbal Ability",
                                                        READING_COMPREHENSION_DEFINITION,
                                                        READING_COMPREHENSION_PROMPT_NORMAL,
                                                        READING_COMPREHENSION_PROMPT_HARD));
                        put("Data Interpretation",
                                        new SubtopicInfo("Data Interpretation", "Data Interpretation",
                                                        DATA_INTERPRETATION_DEFINITION,
                                                        DATA_INTERPRETATION_PROMPT_NORMAL,
                                                        DATA_INTERPRETATION_PROMPT_HARD));
                }
        };

        /**
         * Get subtopic information by name
         */
        public static SubtopicInfo getSubtopicInfo(String subtopicName) {
                return SUBTOPIC_REPO.get(subtopicName);
        }

        /**
         * Get all subtopics in a category
         */
        public static List<String> getSubtopicsByCategory(String category) {
                List<String> subtopics = new ArrayList<>();
                for (SubtopicInfo info : SUBTOPIC_REPO.values()) {
                        if (info.category.equals(category)) {
                                subtopics.add(info.name);
                        }
                }
                return subtopics;
        }

        /**
         * Check if a subtopic exists
         */
        public static boolean isValidSubtopic(String subtopicName) {
                return SUBTOPIC_REPO.containsKey(subtopicName);
        }

        /**
         * Get all categories
         */
        public static Set<String> getAllCategories() {
                Set<String> categories = new LinkedHashSet<>();
                for (SubtopicInfo info : SUBTOPIC_REPO.values()) {
                        categories.add(info.category);
                }
                return categories;
        }

        /**
         * Inner class to hold subtopic information
         */
        public static class SubtopicInfo {
                public final String name;
                public final String category;
                public final String definition;
                public final String promptNormal;
                public final String promptHard;

                public SubtopicInfo(String name, String category, String definition, String promptNormal,
                                String promptHard) {
                        this.name = name;
                        this.category = category;
                        this.definition = definition;
                        this.promptNormal = promptNormal;
                        this.promptHard = promptHard;
                }

                @Override
                public String toString() {
                        return name + " (" + category + ")";
                }
        }
}
