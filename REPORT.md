Name: Rutuja Hemant Shastri 
Student ID: 801484366 
Email: rshastri@charlotte.edu

1. Design

I chose Design A — one document per key, compare in the reducer. This design matches the recommended design in the assignment and the structure of the provided class skeletons. The main idea is to group each document by its document ID, send all documents to one reducer, store their distinct word sets, and then compare every pair after all documents have been received.

Mapper

The Mapper reads one input line at a time. The first whitespace-delimited token is treated as the document ID and the remaining text is treated as the document contents. I applied the required tokenization rules: the text is converted to lowercase, split on whitespace, every character that is not a lowercase letter from a-z or a digit from 0-9 is removed, and empty tokens are discarded. The resulting words are stored as a set so that repeated occurrences of the same word count only once.

The Mapper emits the following logical key-value pair:

Key: document ID
Value: the distinct words contained in that document

For example, conceptually, a document would be represented as:

Doc01 -> hadoop spark data cloud
Reducer

I configured exactly one reducer because Design A requires every document to reach the same reducer. For each document ID, the reducer receives the Mapper's word list and reconstructs it as a set. The reducer stores the resulting document ID and word set in an in-memory map.

The reducer cannot calculate a pairwise similarity during an individual reduce() call because the other documents may not have been processed yet. Therefore, the pairwise comparison is performed in cleanup(), which Hadoop calls after the final reduce() call.

In cleanup(), I sort the document IDs and compare every unique pair. For each pair, I create the intersection of the two sets and the union of the two sets. Jaccard similarity is then calculated as:

J(A, B) = |A ∩ B| / |A ∪ B|

It is 1.0 when the two sets are identical and 0.0 when they share nothing.

Pairs with an empty intersection are not written. The remaining similarities are formatted to two decimal places using the US locale, and the document IDs are written in ascending String order.

Driver

The Driver configures the Hadoop job and specifies the Mapper, Reducer, input/output types, and the number of reducers. The important settings beyond the basic L4 Controller structure are:

Setting	Value
setJarByClass	DocumentSimilarityDriver.class
setMapperClass	DocumentSimilarityMapper.class
setReducerClass	DocumentSimilarityReducer.class
setNumReduceTasks	1
setMapOutputKeyClass	Text.class
setMapOutputValueClass	Text.class
setOutputKeyClass	Text.class
setOutputValueClass	NullWritable.class

I used the complete required output line as the output key and NullWritable as the output value. This avoids an unwanted tab separator between the document pair and the word Similarity: in the final TextOutputFormat output. I did not use a Combiner because the Design A operation is not an appropriate associative and commutative aggregation for a Combiner.

2. How I Ran It

I followed the assignment workflow using the provided Docker Hadoop cluster and Maven project.

bash
mvn clean package
docker compose up -d
docker compose ps

docker cp target/DocumentSimilarity-0.0.1-SNAPSHOT.jar resourcemanager:/tmp/
docker cp shared-folder/input/data/small_dataset.txt resourcemanager:/tmp/
docker cp shared-folder/input/data/dataset.txt resourcemanager:/tmp/

docker exec -it resourcemanager bash
bash
hadoop fs -mkdir -p /input/data
hadoop fs -put /tmp/small_dataset.txt /input/data
hadoop fs -put /tmp/dataset.txt /input/data
hadoop fs -ls /input/data
bash
hadoop jar /tmp/DocumentSimilarity-0.0.1-SNAPSHOT.jar \
  com.example.controller.DocumentSimilarityDriver /input/data/small_dataset.txt /output/small_dataset
hadoop fs -cat /output/small_dataset/*

hadoop jar /tmp/DocumentSimilarity-0.0.1-SNAPSHOT.jar \
  com.example.controller.DocumentSimilarityDriver /input/data/dataset.txt /output/dataset
hadoop fs -cat /output/dataset/*
Verification

The Maven build completed successfully and produced target/DocumentSimilarity-0.0.1-SNAPSHOT.jar. The Hadoop cluster started successfully. The small dataset job completed successfully with one mapper and one reducer, and the output matched the required three lines exactly. The full dataset job also completed successfully with one mapper and one reducer. Its counters reported 12 map input records and 66 reduce output records.

Deviation and Fix

I initially tried to load the small dataset with the relative path ./small_dataset.txt from inside the ResourceManager container. Hadoop returned:

put: `./small_dataset.txt': No such file or directory

The datasets had actually been copied to /tmp inside the container. I corrected the command by using the actual file location:

bash
hadoop fs -put /tmp/small_dataset.txt /input/data
hadoop fs -put /tmp/dataset.txt /input/data

After this correction, both files were successfully loaded into HDFS and both MapReduce jobs ran successfully.

3. Output
Small Dataset — 3 lines
Document1, Document2 Similarity: 0.18
Document1, Document3 Similarity: 0.20
Document2, Document3 Similarity: 0.10
Full Dataset — 66 lines
Document 1	Document 2	Similarity
Doc01	Doc02	0.16
Doc01	Doc03	0.13
Doc01	Doc04	0.07
Doc01	Doc05	0.10
Doc01	Doc06	0.09
Doc01	Doc07	0.11
Doc01	Doc08	0.10
Doc01	Doc09	0.11
Doc01	Doc10	0.09
Doc01	Doc11	0.07
Doc01	Doc12	0.19
Doc02	Doc03	0.20
Doc02	Doc04	0.13
Doc02	Doc05	0.10
Doc02	Doc06	0.09
Doc02	Doc07	0.06
Doc02	Doc08	0.09
Doc02	Doc09	0.05
Doc02	Doc10	0.10
Doc02	Doc11	0.06
Doc02	Doc12	0.14
Doc03	Doc04	0.17
Doc03	Doc05	0.11
Doc03	Doc06	0.08
Doc03	Doc07	0.16
Doc03	Doc08	0.11
Doc03	Doc09	0.07
Doc03	Doc10	0.10
Doc03	Doc11	0.12
Doc03	Doc12	0.11
Doc04	Doc05	0.09
Doc04	Doc06	0.11
Doc04	Doc07	0.18
Doc04	Doc08	0.09
Doc04	Doc09	0.08
Doc04	Doc10	0.10
Doc04	Doc11	0.09
Doc04	Doc12	0.09
Doc05	Doc06	0.20
Doc05	Doc07	0.14
Doc05	Doc08	0.15
Doc05	Doc09	0.07
Doc05	Doc10	0.13
Doc05	Doc11	0.14
Doc05	Doc12	0.11
Doc06	Doc07	0.17
Doc06	Doc08	0.15
Doc06	Doc09	0.08
Doc06	Doc10	0.10
Doc06	Doc11	0.12
Doc06	Doc12	0.13
Doc07	Doc08	0.15
Doc07	Doc09	0.07
Doc07	Doc10	0.08
Doc07	Doc11	0.12
Doc07	Doc12	0.11
Doc08	Doc09	0.19
Doc08	Doc10	0.13
Doc08	Doc11	0.22
Doc08	Doc12	0.12
Doc09	Doc10	0.13
Doc09	Doc11	0.12
Doc09	Doc12	0.13
Doc10	Doc11	0.12
Doc10	Doc12	0.12
Doc11	Doc12	0.11
4. Analysis

The highest similarity in the dataset is Doc08 and Doc11, with a Jaccard similarity of 0.22. Doc08 describes Apache Spark as a distributed processing engine, including Spark jobs, transformations, resilient distributed datasets, the driver, and executors. Doc11 describes Spark MLlib for distributed machine learning, including classification, regression, clustering, pipelines, feature transformers, and models. Because both documents specifically discuss Spark and distributed processing, their relatively high similarity is consistent with their topics.

Other relatively high similarities also correspond to related topics. Doc02 and Doc03 have a similarity of 0.20; Doc02 discusses virtualization and virtual machines while Doc03 discusses containers and explicitly contrasts them with virtual machines. Doc05 and Doc06 have a similarity of 0.20 because both discuss Hadoop and big-data processing, with Doc06 focusing on MapReduce. Doc01 and Doc12 have a similarity of 0.19 because both discuss cloud computing and cloud services. Doc08 and Doc09 also have a similarity of 0.19 because both are specifically about Spark.

The lowest similarity is Doc02 and Doc09 at 0.05. Doc02 focuses on virtualization, hypervisors, virtual machines, physical servers, and isolation, while Doc09 focuses on Spark DataFrames, Spark SQL, and the Catalyst optimizer. Their main topics are quite different, so their low word overlap is reasonable.

The similarity values are generally low because Jaccard similarity uses the size of the intersection divided by the size of the union. Even related documents contain many words that do not occur in the other document, which makes the union much larger than the intersection. In addition, the required tokenization treats common English words such as "the," "and," "is," "a," "on," and "with" as ordinary tokens. These words can appear in many documents without being strong indicators of topic similarity.

One tokenization change that could make the numbers more meaningful would be to remove common stop words before constructing the document sets. This would reduce the influence of words that occur frequently across many documents and allow topic-specific words such as Spark, Hadoop, containers, virtualization, and MapReduce to have a greater effect on the similarity.

This proposed change is only an analysis of a possible alternative tokenization rule. I kept the implementation consistent with the assignment's required tokenization rules so that the submitted output remains comparable to the expected results.

5. Scalability

Design A works well for the provided dataset because the single reducer can store the 12 documents and their distinct-word sets in memory. However, the design does not scale well to a collection containing one million documents.

First, the reducer must keep every document's word set in memory before it can perform the comparisons. As the number of documents increases, the amount of memory required by the single reducer also increases. For a sufficiently large collection, the reducer could run out of memory and fail.

Second, Design A performs every possible pairwise comparison. For N documents, the number of pairs is N(N - 1) / 2. With one million documents, this is 499,999,500,000 pairs. Performing that many comparisons in one reducer is not practical.

Design B addresses the single-reducer bottleneck by changing the grouping key. Instead of grouping by document, the Mapper emits each distinct word together with the document ID containing it. The shuffle can then distribute different words across multiple reducers. A reducer can generate document pairs that share a word and count the shared words, which gives the intersection size for each pair.

To compute Jaccard similarity, the document sizes are also required. Therefore, Design B generally needs a second MapReduce pass or another mechanism to carry document sizes through the computation. Its main advantage is that the word-based work can be distributed across multiple reducers rather than requiring one reducer to hold the entire document collection.

6. Problems and Fixes

The main execution problem I encountered was an incorrect local path when loading the small dataset into HDFS.

Command that failed:

bash
hadoop fs -put ./small_dataset.txt /input/data

Actual error:

put: `./small_dataset.txt': No such file or directory

The cause was that the dataset had been copied to /tmp inside the ResourceManager container. I corrected the command by using /tmp/small_dataset.txt. I then loaded both datasets successfully and verified them with hadoop fs -ls /input/data.

During the MapReduce runs, Hadoop also displayed a warning about command-line option parsing and ToolRunner. The warning did not cause a job failure: both MapReduce jobs completed successfully, and the required outputs were produced. I therefore did not change the working implementation based on that warning.

7. Use of Generative AI

I used a generative AI tool as a learning and development aid during this assignment. I used it to help me understand the assignment requirements, reason about the two MapReduce designs, understand the Mapper, Reducer, and Driver responsibilities, troubleshoot Hadoop and Docker command-line issues, and review the structure and wording of my report.

I personally ran the Maven build, started the Hadoop Docker cluster, loaded the datasets into HDFS, executed both MapReduce jobs, inspected the Hadoop counters and output, and verified that the small dataset matched the required output and that the full dataset produced 66 lines.
