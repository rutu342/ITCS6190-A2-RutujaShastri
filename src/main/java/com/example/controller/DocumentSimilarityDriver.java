package com.example.controller;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.example.DocumentSimilarityMapper;
import com.example.DocumentSimilarityReducer;

/**
 * Configures and submits the document similarity job.
 *
 * Usage:  hadoop jar DocumentSimilarity-0.0.1-SNAPSHOT.jar \
 *             com.example.controller.DocumentSimilarityDriver <input path> <output path>
 *
 * Compare with Controller.java from Hands-on L4: the structure is the same. Things to think
 * about that were not an issue in L4:
 *
 *   - Number of reducers. With the design suggested in README.md every document must end up
 *     in the SAME reducer, so the job needs exactly one:  job.setNumReduceTasks(1).
 *
 *   - Output separator. TextOutputFormat writes "key<TAB>value". The required output has a
 *     single space between the pair and the word "Similarity", so either put the whole line
 *     in the key and emit NullWritable as the value, or tell Hadoop to use a space:
 *         conf.set("mapreduce.output.textoutputformat.separator", " ");
 *     (this must be set on the Configuration BEFORE Job.getInstance is called).
 *
 *   - No combiner. A combiner only makes sense when the reduce function is associative and
 *     commutative on the mapper's output. Think about whether that is true for your design;
 *     with the suggested one, it is not.
 */
public class DocumentSimilarityDriver {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: DocumentSimilarityDriver <input path> <output path>");
            System.exit(2);
        }

        Configuration conf = new Configuration();
        // TODO: configure the job — see the class comment above and Controller.java from L4.
        Job job = Job.getInstance(conf, "document similarity");

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
