package com.example;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Mapper for the document similarity job.
 *
 * Input:  one line of the input file per call. Each line is one document:
 *         "<DocumentID> <text of the document...>"
 *
 * Output:
 *         DocumentID -> unique words in the document
 */
public class DocumentSimilarityMapper
        extends Mapper<LongWritable, Text, Text, Text> {

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        // Get the complete input line
        String line = value.toString().trim();

        // Ignore empty lines
        if (line.isEmpty()) {
            return;
        }

        // Split into:
        // parts[0] = Document ID
        // parts[1] = document text
        String[] parts = line.split("\\s+", 2);

        // Ignore malformed lines without document text
        if (parts.length < 2) {
            return;
        }

        String documentId = parts[0];

        // Convert document text to lowercase
        String text = parts[1].toLowerCase();

        // A Set removes duplicate words
        Set<String> words = new HashSet<>();

        // Split the document text on whitespace
        for (String token : text.split("\\s+")) {

            // Remove every character that is not a-z or 0-9
            token = token.replaceAll("[^a-z0-9]", "");

            // Ignore empty tokens
            if (!token.isEmpty()) {
                words.add(token);
            }
        }

        // Emit:
        // key   = Document ID
        // value = unique words
        context.write(
            new Text(documentId),
            new Text(String.join(" ", words))
        );
    }
}