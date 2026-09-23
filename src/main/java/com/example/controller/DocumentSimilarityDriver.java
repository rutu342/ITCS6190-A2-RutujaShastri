package com.example.controller;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
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
 */
public class DocumentSimilarityDriver {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.err.println(
                "Usage: DocumentSimilarityDriver <input path> <output path>"
            );
            System.exit(2);
        }

        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf, "document similarity");

        // Tell Hadoop which class contains the main() method
        job.setJarByClass(DocumentSimilarityDriver.class);

        // Set Mapper and Reducer
        job.setMapperClass(DocumentSimilarityMapper.class);
        job.setReducerClass(DocumentSimilarityReducer.class);

        // Design A requires exactly one reducer
        job.setNumReduceTasks(1);

        // Mapper output types
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);

        // Final output types
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        // Input and output paths
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}