package com.example;

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

/**
 * Mapper for the document similarity job.
 *
 * Input:  one line of the input file per call. Each line is one document:
 *         "<DocumentID> <text of the document...>"
 *         The key is the byte offset of the line in the file (you will not need it).
 *
 * Output: TODO — decide what your mapper emits. Whatever you choose, the reducer must be
 *         able to reconstruct, for every pair of documents, how many distinct words the two
 *         documents share and how many distinct words they have in total.
 *
 * Tokenization rules (the same for everyone, so outputs are comparable):
 *   - the document ID is the first whitespace-delimited token of the line
 *   - convert the rest of the line to lower case and split it on whitespace
 *   - strip every character that is not a-z or 0-9 from each token ("Hadoop," -> "hadoop")
 *   - drop tokens that are empty after stripping
 *   - a document is the SET of its tokens: a word that appears twice counts once
 *
 * The generic types below match the design suggested in README.md. You may change them if
 * you choose a different design — just keep them consistent with the reducer and driver.
 */
public class DocumentSimilarityMapper extends Mapper<LongWritable, Text, Text, Text> {

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
        // TODO: split the line into the document ID and the text,
        //       tokenize the text following the rules above,
        //       and emit what the reducer needs.
    }
}
