# Assignment #2: Document Similarity with MapReduce

**ITCS 6190/8190 — Cloud Computing for Data Analysis — Fall 2026**

In Hands-on L4 you ran a MapReduce job somebody else had written. In this assignment you
write one. The job reads a collection of documents and computes, for every pair of
documents, how similar they are — using the Jaccard similarity of their word sets. The
cluster, the Maven build, the class skeletons and two datasets are given; the Mapper, the
Reducer and the job configuration are yours to write.

By the end you should be able to take a problem that is not a word count, decide what the
keys and values should be, and turn that decision into a working Hadoop job.

**Worth 2 points** (the rubric below is out of 100 and is scaled to 2). Opens September 8.
**Due by 11:59 pm on Tuesday, September 22**, on Canvas. Work individually.

---

## The problem

Two documents are similar if they use the same words. The **Jaccard similarity** of two
sets *A* and *B* is the size of their intersection divided by the size of their union:

```
J(A, B) = |A ∩ B| / |A ∪ B|
```

It is 1.0 when the two sets are identical and 0.0 when they share nothing.

Take the three documents in `shared-folder/input/data/small_dataset.txt`:

```
Document1 This is a sample document containing words
Document2 Another document that also has words
Document3 Sample text with different words
```

After tokenization (rules below) the word sets are:

| Document | Words | Size |
| -------- | ----- | ---- |
| Document1 | a, containing, document, is, sample, this, words | 7 |
| Document2 | also, another, document, has, that, words | 6 |
| Document3 | different, sample, text, with, words | 5 |

Document1 and Document2 share `document` and `words`, so the intersection has 2 words; the
union has 7 + 6 − 2 = 11. Their similarity is 2 / 11 = **0.18**. Working through the other
two pairs gives the expected output of the job:

```
Document1, Document2 Similarity: 0.18
Document1, Document3 Similarity: 0.20
Document2, Document3 Similarity: 0.10
```

Your job must produce exactly this for the small dataset, and the equivalent 66 lines for
the 12-document `dataset.txt`.

### Input format

A plain text file with **one document per line**. The first whitespace-delimited token of
the line is the document ID; everything after it is the document's text. IDs contain no
spaces.

### Tokenization rules

Everybody uses the same rules, so that outputs are comparable:

1. Convert the text to lower case and split it on whitespace.
2. Strip every character that is not `a`–`z` or `0`–`9` from each token, so `Hadoop,`
   becomes `hadoop` and `key-value` becomes `keyvalue`.
3. Drop tokens that are empty after stripping.
4. A document is the **set** of its tokens. A word that appears three times counts once.

### Output format

One line per pair of documents that share at least one word, in exactly this form:

```
Doc01, Doc02 Similarity: 0.18
```

- The two IDs are in ascending `String` order (`Doc01` before `Doc02`) and separated by a
  comma and a space.
- A single space, then the word `Similarity:`, a space, and the similarity rounded to
  **two decimals**.
- Pairs that share no word are not printed. (With the two datasets provided, every pair
  shares at least one word, so you should see 3 and 66 lines respectively.)
- The order of the lines does not matter.

---

## What is in this repository

| Path | What it is |
| ---- | ---------- |
| `src/main/java/com/example/DocumentSimilarityMapper.java` | **you write this** — skeleton with the method signature and the tokenization rules in a comment |
| `src/main/java/com/example/DocumentSimilarityReducer.java` | **you write this** — skeleton |
| `src/main/java/com/example/controller/DocumentSimilarityDriver.java` | **you complete this** — reads the arguments and submits the job; the job configuration is a `TODO` |
| `pom.xml` | the Maven build, same as L4; produces `target/DocumentSimilarity-0.0.1-SNAPSHOT.jar` |
| `docker-compose.yml`, `config` | the cluster, identical to L4: NameNode, 3 DataNodes, ResourceManager, 2 NodeManagers, history server, on the official `apache/hadoop:3` image |
| `shared-folder/input/data/small_dataset.txt` | 3 documents — use it to check your output against the expected result above |
| `shared-folder/input/data/dataset.txt` | 12 documents about the topics of this course — the dataset you report on |
| `REPORT.md` | the report template you fill in |
| `SETUP.md` | installing Java and Maven on Windows, macOS or Linux |

---

## Designing the job

Word count was easy to map onto MapReduce because the answer for each word depends only on
that word. Similarity is different: the answer for a pair of documents depends on *both*
documents, and the mapper only ever sees one line at a time. Deciding how to get the right
information together in one reducer is the whole assignment. Two designs work.

### Design A — one document per key, compare in the reducer (recommended)

The mapper reads one document, applies the tokenization rules, and emits
`(documentID, "word1 word2 word3 ...")` — the ID as the key and the document's distinct words
as the value. The driver sets **exactly one reducer**, so every document arrives at the same
reducer, one per `reduce()` call. `reduce()` cannot compare anything yet, because it has not
seen the other documents, so it just stores the word set in a map. When Hadoop calls
`cleanup()` after the last `reduce()`, the reducer has every document, loops over all pairs,
computes the Jaccard similarity, and writes one line per pair.

This is simple and it is what the skeletons are set up for. It also has an obvious weakness:
a single reducer holding every document in memory does not scale. Your report asks you to
explain why, and how you would fix it.

### Design B — one word per key, count shared words

The mapper emits `(word, documentID)` for every distinct word of a document — an inverted
index, like a word count where the value is the document instead of `1`. The reducer for a
word receives the list of documents containing it and emits `(docA, docB) → 1` for every pair
in that list. Summing those 1s per pair gives `|A ∩ B|`. To turn that into Jaccard you also
need `|A|` and `|B|`, which means either a second MapReduce job, or emitting each document's
size alongside its words and carrying it through. This design parallelises across many
reducers and is how you would do it for real. It is more work; choose it if you want the
challenge. Both designs receive full credit when correct.

Whichever design you pick, `REPORT.md` must explain it in your own words: what the mapper
emits and why, what the reducer receives, and how the Jaccard value is computed.

### Things that were not an issue in L4

- **Number of reducers.** Design A only works with `job.setNumReduceTasks(1)`. If you see
  some pairs but not all, this is why.
- **Output separator.** `TextOutputFormat` writes `key<TAB>value`. The required output has a
  space between the pair and `Similarity:`. Either set
  `conf.set("mapreduce.output.textoutputformat.separator", " ")` on the `Configuration`
  *before* `Job.getInstance(...)`, or put the whole line in the key and emit
  `NullWritable.get()` as the value.
- **Formatting.** `String.format("%.2f", x)` follows the machine's locale and may print
  `0,18` on some systems. Use `String.format(java.util.Locale.US, "%.2f", x)`.
- **Combiner.** L4 reused the reducer as a combiner. Do not do that here unless you can argue
  it is correct for your design — for Design A it is not.

---

## Prerequisites

Same as L4: **Docker Desktop** running, plus a **JDK** and **Maven** (see [SETUP.md](SETUP.md)).

```bash
docker --version
java -version
mvn -version
```

---

## Steps

The cycle is the one you practised in L4 — build, deploy, load data, submit, read output —
with your own code in the JAR.

### 1. Write the code

Complete the three classes under `src/main/java`. The comments in each file tell you what is
expected. Build often; a job that does not compile is easier to fix than one that fails on
the cluster.

### 2. Start the Hadoop cluster

```bash
docker compose up -d
```

Give it a minute, then check <http://localhost:9870> shows three live DataNodes.

### 3. Build the project

```bash
mvn clean package
```

This produces `target/DocumentSimilarity-0.0.1-SNAPSHOT.jar`.

### 4. Copy the JAR and the datasets into the ResourceManager container

```bash
docker cp target/DocumentSimilarity-0.0.1-SNAPSHOT.jar resourcemanager:/tmp/
docker cp shared-folder/input/data/small_dataset.txt resourcemanager:/tmp/
docker cp shared-folder/input/data/dataset.txt resourcemanager:/tmp/
```

### 5. Open a shell in the container

```bash
docker exec -it resourcemanager bash
cd /tmp
```

Everything from here until step 9 happens inside the container.

### 6. Load the datasets into HDFS

```bash
hadoop fs -mkdir -p /input/data
hadoop fs -put ./small_dataset.txt /input/data
hadoop fs -put ./dataset.txt /input/data
hadoop fs -ls /input/data
```

### 7. Run the job on the small dataset and check it

```bash
hadoop jar /tmp/DocumentSimilarity-0.0.1-SNAPSHOT.jar \
  com.example.controller.DocumentSimilarityDriver /input/data/small_dataset.txt /output/small_dataset

hadoop fs -cat /output/small_dataset/*
```

Compare with the expected output in [The problem](#the-problem). Do not move on until the
three lines match exactly. Remember the output directory must not already exist: to rerun,
`hadoop fs -rm -r /output/small_dataset` first.

### 8. Run the job on the full dataset

```bash
hadoop jar /tmp/DocumentSimilarity-0.0.1-SNAPSHOT.jar \
  com.example.controller.DocumentSimilarityDriver /input/data/dataset.txt /output/dataset

hadoop fs -cat /output/dataset/*
```

You should get 66 lines.

### 9. Copy the results back to your machine

Inside the container:

```bash
hdfs dfs -get /output /tmp/
exit
```

Then on your machine:

```bash
docker cp resourcemanager:/tmp/output/. shared-folder/output/
```

You should now have `shared-folder/output/small_dataset/part-r-00000` and
`shared-folder/output/dataset/part-r-00000`.

### 10. Stop the cluster

```bash
docker compose down
```

Add `-v` if you also want to discard the HDFS data.

---

## What to commit

- Your **source code** under `src/main/java`
- The **output of both runs** under `shared-folder/output/small_dataset/` and
  `shared-folder/output/dataset/`
- Your completed **`REPORT.md`**

Do not commit the `target/` directory (`.gitignore` already excludes it). Leave this README
and the two input datasets as they are.

---

## Report

Fill in **`REPORT.md`** in the root of this repository. It has these sections; keep each one
short and specific.

### Design
Which design you chose. What your Mapper emits as key and value, and why. What your Reducer
receives, what it does with it, and where the Jaccard similarity is computed. What you had to
set in the Driver beyond what L4's `Controller` set.

### How I ran it
The commands you used, in order. If you deviated from the steps above, say where and why.

### Output
The three lines for `small_dataset.txt` and the 66 lines for `dataset.txt`, pasted in.

### Analysis
Look at the results for `dataset.txt`. Which pairs of documents are the most similar and
which the least? Do the most similar pairs make sense given what the documents are about?
The similarity values are all fairly low and quite close together — why do you think that is,
and what one change to the tokenization would make the numbers more meaningful?

### Scalability
If you used Design A: it needs a single reducer that holds every document in memory. Explain
concretely what breaks when the collection has a million documents, and sketch how Design B
avoids the problem. If you used Design B: explain why it needed more than one pass (or how
you avoided that), and what its own bottleneck is.

### Problems and fixes
What went wrong and what resolved it. Paste the actual error message. If nothing went wrong,
say so.

### Use of generative AI
Per the syllabus, if you used a generative AI tool, include the acknowledgment statement
and say what you used it for. If you did not, say that.

---

## Submission

### 1. Make your own copy of this repository

On the repository page, click the green **Use this template** button, then
**Create a new repository**. Name it `ITCS6190-A2-<your-name>` and set the visibility to
**Public**.

Do not fork, and do not clone this repository directly. A fork or a clone still points at
the course repository, so your work would not end up anywhere we can grade it.

Then clone *your* new repository to your machine and work there.

### 2. Commit your work

Your code, both outputs, and `REPORT.md` — see [What to commit](#what-to-commit).

### 3. Submit the link

Post the URL of **your** repository on Canvas by the deadline. Keep it public until grades
are posted. There is no need to add the instructor or the TAs as collaborators.

---

## Grading rubric

| Criterion | Points |
| --------- | -----: |
| **Correct output on `small_dataset.txt`** — the three lines match exactly, including format | 20 |
| **Correct output on `dataset.txt`** — 66 lines, correct values and pair ordering | 20 |
| **Mapper and Reducer** — implement the tokenization rules as specified; clean, readable code; no unused or copied-in code | 15 |
| **Driver and build** — job configured correctly (reducers, output types, separator); `mvn clean package` builds from a fresh clone | 10 |
| **Report: design** — explains the keys, values and data flow in the student's own words | 15 |
| **Report: analysis and scalability** — specific observations about the results; a correct, concrete scalability argument | 10 |
| **Report: run, problems, reproducibility** — commands, outputs committed where specified, honest problems section | 10 |
| **Total** | **100** |

Late submissions follow the syllabus: −20% after one day, −50% after two, no credit after
three.

---

## Troubleshooting

Most of the L4 troubleshooting still applies; the new ones are first.

- **Output has a tab between the pair and `Similarity:`** — the default `TextOutputFormat`
  separator. See [Things that were not an issue in L4](#things-that-were-not-an-issue-in-l4).
- **Some pairs are missing, or the same pair appears twice** — more than one reducer with
  Design A. Set `job.setNumReduceTasks(1)`.
- **`Doc02, Doc01` instead of `Doc01, Doc02`** — sort the two IDs before writing the pair.
- **Values slightly off (e.g. 0.17 instead of 0.18)** — usually the union was computed as
  `|A| + |B|` without subtracting the intersection, or duplicates within a document were not
  removed.
- **Values very different from expected** — a tokenization rule was skipped. Check the small
  dataset by hand against the table in [The problem](#the-problem).
- **Output is empty** — the mapper is not emitting, or it split the line incorrectly.
  Print a few records with `System.err.println` and look at the task logs at
  <http://localhost:8088>.
- **`ClassNotFoundException: com.example.controller.DocumentSimilarityDriver`** — the JAR in
  the container is stale, or `job.setJarByClass` is missing. Rebuild and repeat step 4.
- **`Output directory already exists`** — delete it with `hadoop fs -rm -r /output/...` or
  write to a new path.
- **`docker cp` says no such container** — the cluster is not running or is still starting.
  Run `docker ps` and look for `resourcemanager`.
- **Job is accepted but never progresses** — the NodeManagers may not have registered yet.
  Check <http://localhost:8088> for active nodes.
- **`UnsupportedClassVersionError`** — `pom.xml` pins `maven.compiler.release` to 8; rerun
  `mvn clean package`. If it persists, build with a JDK 8, 11 or 17.
- **`Permission denied` writing inside the container** — the image runs as the `hadoop`
  user. Work in `/tmp`, as the steps above do.
- **Ports already in use** — something else is on 9870 or 8088. Stop it, or change the port
  mappings in `docker-compose.yml`.
