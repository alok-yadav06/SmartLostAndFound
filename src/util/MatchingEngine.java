package util;

import model.FoundItem;
import model.Item;
import model.LostItem;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║           MatchingEngine — Smart Matching            ║
 * ╚══════════════════════════════════════════════════════╝
 *
 * This is the "intelligent" part of the system.
 * It scores how likely a lost item matches a found item.
 *
 * ALGORITHM: Weighted Scoring
 * Each criterion has a weight. We sum the weights of matched criteria.
 * Score = (matched_weight / total_weight) × 100
 *
 * This is essentially a simplified version of how:
 * - Google ranks search results
 * - Recommendation systems suggest items
 * - Resume screening tools match candidates
 *
 * Real-world systems use ML for this. We use heuristics (rules).
 * Both are valid — heuristics are fast, explainable, and reliable.
 *
 * CONCEPTS USED:
 * - Java Streams (filter, map, sort, collect)
 * - Comparator / Comparable
 * - Inner class (MatchResult)
 * - String algorithms (tokenization, Jaccard similarity)
 */
public class MatchingEngine {

    // Weights for each matching criterion (must sum to 100)
    private static final int WEIGHT_CATEGORY    = 30;
    private static final int WEIGHT_KEYWORDS    = 35;
    private static final int WEIGHT_LOCATION    = 20;
    private static final int WEIGHT_DATE        = 15;

    private static final int MIN_MATCH_SCORE    = 25; // Below this = not a match

    // ── Public API ─────────────────────────────────────────

    /**
     * Given a lost item, find the best matching found items.
     * Returns a list sorted by score (highest first).
     */
    public List<MatchResult> findMatchesForLost(LostItem lost, List<FoundItem> allFound) {
        return allFound.stream()
            .map(found -> new MatchResult(found, computeScore(lost, found)))
            .filter(r -> r.score >= MIN_MATCH_SCORE)
            .sorted(Comparator.comparingInt(MatchResult::getScore).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Given a found item, find possible lost item owners.
     */
    public List<MatchResult> findMatchesForFound(FoundItem found, List<LostItem> allLost) {
        return allLost.stream()
            .map(lost -> new MatchResult(lost, computeScore(lost, found)))
            .filter(r -> r.score >= MIN_MATCH_SCORE)
            .sorted(Comparator.comparingInt(MatchResult::getScore).reversed())
            .collect(Collectors.toList());
    }

    // ── Scoring Logic ──────────────────────────────────────

    private int computeScore(LostItem lost, FoundItem found) {
        int score = 0;

        // 1. Category match (exact = full points)
        score += categoryScore(lost.getCategory(), found.getCategory());

        // 2. Keyword match (name + description tokenized)
        score += keywordScore(lost, found);

        // 3. Location proximity (fuzzy string match)
        score += locationScore(lost.getLocation(), found.getLocation());

        // 4. Date proximity (found after lost is reported = more likely)
        score += dateScore(lost, found);

        return Math.min(score, 100); // cap at 100
    }

    private int categoryScore(String cat1, String cat2) {
        if (cat1 == null || cat2 == null) return 0;
        return cat1.equalsIgnoreCase(cat2) ? WEIGHT_CATEGORY : 0;
    }

    private int keywordScore(LostItem lost, FoundItem found) {
        // Tokenize both name+description into lowercase word sets
        Set<String> lostWords  = tokenize(lost.getName()  + " " + lost.getDescription());
        Set<String> foundWords = tokenize(found.getName() + " " + found.getDescription());

        if (lostWords.isEmpty() || foundWords.isEmpty()) return 0;

        // Jaccard Similarity: |intersection| / |union|
        // Measures overlap between two sets
        Set<String> intersection = new HashSet<>(lostWords);
        intersection.retainAll(foundWords);

        Set<String> union = new HashSet<>(lostWords);
        union.addAll(foundWords);

        double jaccard = (double) intersection.size() / union.size();
        return (int) (jaccard * WEIGHT_KEYWORDS);
    }

    private int locationScore(String loc1, String loc2) {
        if (loc1 == null || loc2 == null) return 0;
        loc1 = loc1.toLowerCase().trim();
        loc2 = loc2.toLowerCase().trim();

        if (loc1.equals(loc2))                      return WEIGHT_LOCATION;
        if (loc1.contains(loc2) || loc2.contains(loc1)) return WEIGHT_LOCATION / 2;

        // Check word overlap
        Set<String> w1 = tokenize(loc1);
        Set<String> w2 = tokenize(loc2);
        Set<String> common = new HashSet<>(w1);
        common.retainAll(w2);
        if (!common.isEmpty()) return WEIGHT_LOCATION / 3;

        return 0;
    }

    private int dateScore(LostItem lost, FoundItem found) {
        if (lost.getDate() == null || found.getDate() == null) return 0;

        // Found item should be reported ON or AFTER the lost date
        // and within 30 days
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            lost.getDate(), found.getDate()
        );

        if (daysBetween >= 0 && daysBetween <= 3)  return WEIGHT_DATE;       // Same day-ish
        if (daysBetween >= 0 && daysBetween <= 7)  return WEIGHT_DATE * 2/3; // Within a week
        if (daysBetween >= 0 && daysBetween <= 30) return WEIGHT_DATE / 3;   // Within a month
        if (daysBetween < 0 && daysBetween >= -7)  return WEIGHT_DATE / 4;   // Found before reported (possible)
        return 0;
    }

    // ── Text Utilities ─────────────────────────────────────

    /** Splits text into meaningful words, removes stopwords */
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();

        Set<String> stopWords = Set.of(
            "a","an","the","is","it","in","on","at","to","of","and","or","my","i"
        );

        return Arrays.stream(text.toLowerCase().split("[\\s,;.!?]+"))
            .filter(w -> w.length() > 2)
            .filter(w -> !stopWords.contains(w))
            .collect(Collectors.toSet());
    }

    // ── Inner Class: Match Result ──────────────────────────

    /**
     * Encapsulates a matched item with its score.
     * WHY INNER CLASS? It's tightly coupled to MatchingEngine's logic.
     * It has no meaning outside this context.
     *
     * CONCEPT: Static inner class = doesn't need outer instance.
     * Non-static inner class = can access outer fields (avoid for MatchResult).
     */
    public static class MatchResult {
        private final Item item;
        private final int score;

        public MatchResult(Item item, int score) {
            this.item  = item;
            this.score = score;
        }

        public Item getItem()  { return item; }
        public int  getScore() { return score; }

        public String getScoreLabel() {
            if (score >= 75) return "🔥 Excellent Match";
            if (score >= 50) return "✅ Good Match";
            if (score >= 25) return "🔍 Possible Match";
            return "Low Match";
        }

        @Override
        public String toString() {
            return item.getName() + " [" + score + "% — " + getScoreLabel() + "]";
        }
    }
}