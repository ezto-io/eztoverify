package com.qlikverify.backoffice.helpers.nameSimilarityCheckHelper;

import org.apache.commons.codec.language.Caverphone2;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Stream;

public class NameSimilarityCheckHelper {
    private static final Logger logger = Logger.getLogger(NameSimilarityCheckHelper.class);

    private static final double JARO_WINKLER_WEIGHT = 30.0;
    private static final double LEVENSHTEIN_WEIGHT = 30.0;
    private static final double METAPHONE_WEIGHT = 10.0;
    private static final double SORTED_MATCH_WEIGHT = 30.0;

    public static double calculateNameSimilarity(String name1, String name2) {

        name1 = expandInitials(name1);
        name1 = removeTitles(name1);
        name2 = expandInitials(name2);
        name2 = removeTitles(name2);
        String processedName1 = preProcessNames(name1, false);
        String processedName2 = preProcessNames(name2, false);
        String sortedName1 = preProcessNames(name1, true);
        String sortedName2 = preProcessNames(name2, true);

        if (Stream.of(processedName1, processedName2, sortedName1, sortedName2).anyMatch(String::isEmpty)) {
            return 0.0;
        }

        double levenshteinSimilarity = calculateLevenshteinSimilarity(processedName1, processedName2);
        double jaroWinklerSimilarity = calculateJaroWinklerSimilarity(processedName1, processedName2);
        double doubleMetaphoneScore = calculateDoubleMetaphoneScore(processedName1, processedName2);
        double sortedNameScore = calculateSortedMatchScore(sortedName1, sortedName2);

        return calculateFinalScore(jaroWinklerSimilarity, levenshteinSimilarity,
                doubleMetaphoneScore, sortedNameScore);
    }

    private static String preProcessNames(String name, boolean sort) {
        String[] words = name.toLowerCase()
                .replaceAll("\\.", " ")
                .replaceAll("[^a-z\\s]", "")
                .trim()
                .split("\\s+");

        if (sort) {
            Arrays.sort(words);
        }
        return String.join(" ", words);
    }

    private static double calculateSortedMatchScore(String s1, String s2) {

        if (s1.equalsIgnoreCase(s2)) {
            return 1.0;
        }
        Set<String> words1 = new HashSet<>(Arrays.asList(s1.split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(s2.split("\\s+")));

        if (words1.equals(words2)) {
            return 1.0;
        }

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        double wordSimilarity = (double) intersection.size() / union.size();
        int[] freq1 = getCharacterFrequency(s1);
        int[] freq2 = getCharacterFrequency(s2);

        int matchingChars = 0;
        for (int i = 0; i < 26; i++) {
            matchingChars += Math.min(freq1[i], freq2[i]);
        }

        double charSimilarity = (double) matchingChars /
                Math.max(s1.replaceAll("\\s", "").length(), s2.replaceAll("\\s", "").length());
        return (wordSimilarity * 0.7) + (charSimilarity * 0.3);
    }

    private static int[] getCharacterFrequency(String str) {
        int[] freq = new int[26];
        for (char c : str.toCharArray()) {
            if (Character.isLetter(c)) {
                freq[c - 'a']++;
            }
        }
        return freq;
    }

    private static double calculateFinalScore(double jaroWinklerScore, double levenshteinScore,
                                              double metaphoneScore, double sortedScore) {
        double score = ((jaroWinklerScore * (JARO_WINKLER_WEIGHT / 100.0)) +
                (levenshteinScore * (LEVENSHTEIN_WEIGHT / 100.0)) +
                (metaphoneScore * (METAPHONE_WEIGHT / 100.0)) +
                (sortedScore * (SORTED_MATCH_WEIGHT / 100.0))) * 100;

        return Math.round(score * 100.0) / 100.0;
    }

    private static double calculateJaroWinklerSimilarity(String s1, String s2) {
        return new JaroWinklerSimilarity().apply(s1, s2);
    }

    private static double calculateLevenshteinSimilarity(String s1, String s2) {
        int distance = LevenshteinDistance.getDefaultInstance().apply(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        if(maxLength == 0) {
            return 0.0;
        }
        return 1.0 - ((double) distance / maxLength);
    }

    private static double calculateDoubleMetaphoneScore(String s1, String s2) {
        DoubleMetaphone doubleMetaphone = new DoubleMetaphone();
        Soundex soundex = new Soundex();
        Caverphone2 caverphone = new Caverphone2();

        try {
            boolean doubleMetaphoneMatch = doubleMetaphone.doubleMetaphone(s1)
                    .equals(doubleMetaphone.doubleMetaphone(s2));

            boolean soundexMatch = soundex.encode(s1).equals(soundex.encode(s2));
            boolean caverphoneMatch = caverphone.encode(s1).equals(caverphone.encode(s2));

            return ((doubleMetaphoneMatch ? 1.0 : 0.0) * 0.5)
                    + (soundexMatch ? 1.0 : 0.0) * 0.2
                    + (caverphoneMatch ? 1.0 : 0.0) * 0.3;
        } catch (NullPointerException e) {
            return 0.0;
        }
    }

    private static final Set<String> COMMON_TITLES = Set.of(
            "mr", "mrs", "ms", "miss", "dr", "prof", "sri", "shri", "mt", "kum", "sm"
    );

    private static String removeTitles(String name) {
        List<String> words = new ArrayList<>(Arrays.asList(name.toLowerCase().split("\\s+")));
        words.removeAll(COMMON_TITLES);
        return String.join(" ", words);
    }

    private static String expandInitials(String name) {
        String[] words = name.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() == 1) { // If it's a single character (initial)
                words[i] = words[i] + "."; // Ensure it's treated as an initial
            }
        }
        return String.join(" ", words);
    }


}
