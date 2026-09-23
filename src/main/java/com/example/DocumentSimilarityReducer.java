package com.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

/**
 * Reducer for the document similarity job.
 *
 * The reducer stores each document as a set of words.
 * After all documents have been received, cleanup()
 * compares every pair of documents and calculates
 * their Jaccard similarity.
 */
public class DocumentSimilarityReducer
        extends Reducer<Text, Text, Text, NullWritable> {

    // Stores:
    // Document ID -> Set of unique words
    private final Map<String, Set<String>> documents = new HashMap<>();

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        String documentId = key.toString();

        // Reconstruct the set of words for this document
        for (Text value : values) {

            Set<String> words = new HashSet<>();

            String wordList = value.toString().trim();

            if (!wordList.isEmpty()) {
                for (String word : wordList.split("\\s+")) {
                    words.add(word);
                }
            }

            documents.put(documentId, words);
        }
    }

    @Override
    protected void cleanup(Context context)
            throws IOException, InterruptedException {

        // Put document IDs into sorted order
        List<String> documentIds =
                new ArrayList<>(documents.keySet());

        Collections.sort(documentIds);

        // Compare every unique pair of documents
        for (int i = 0; i < documentIds.size(); i++) {

            for (int j = i + 1; j < documentIds.size(); j++) {

                String documentA = documentIds.get(i);
                String documentB = documentIds.get(j);

                Set<String> wordsA = documents.get(documentA);
                Set<String> wordsB = documents.get(documentB);

                // Calculate intersection
                Set<String> intersection =
                        new HashSet<>(wordsA);

                intersection.retainAll(wordsB);

                // Assignment says to omit pairs with zero overlap
                if (intersection.isEmpty()) {
                    continue;
                }

                // Calculate union
                Set<String> union =
                        new HashSet<>(wordsA);

                union.addAll(wordsB);

                // Jaccard similarity
                double similarity =
                        (double) intersection.size() / union.size();

                // Format to exactly two decimal places
                String formattedSimilarity =
                        String.format(
                                Locale.US,
                                "%.2f",
                                similarity
                        );

                // Required output format
                String output =
                        documentA + ", "
                        + documentB
                        + " Similarity: "
                        + formattedSimilarity;

                // Entire output line is the key.
                // Value is NullWritable.
                context.write(
                        new Text(output),
                        NullWritable.get()
                );
            }
        }
    }
}