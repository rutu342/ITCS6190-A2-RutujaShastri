# Assignment #2 — Report

**Name:**
**Student ID:**
**Email:**

---

## Design

Which design did you choose (A, B, or your own)? Explain in your own words:

- What your **Mapper** emits as key and value, and why that is the right thing to emit.
- What your **Reducer** receives for one key, what it does with it, and where the Jaccard
  similarity is computed.
- What you had to set in the **Driver** beyond what L4's `Controller` set, and why.



---

## How I ran it

The commands you used, in the order you used them. If you deviated from the steps in the
README, say where and why.

```bash

```

---

## Output

### `small_dataset.txt` (3 lines)

```

```

### `dataset.txt` (66 lines)

```

```

---

## Analysis

Look at the results for `dataset.txt`.

- Which pairs are the most similar, and which the least?
- Do the most similar pairs make sense given what the documents are about?
- The values are all fairly low and close together. Why? What one change to the tokenization
  rules would make the numbers more meaningful?



---

## Scalability

**If you used Design A:** it relies on a single reducer that holds every document in memory.
What concretely breaks when the collection has a million documents? Sketch how Design B
avoids the problem.

**If you used Design B:** why did it need more than one pass (or how did you avoid that)?
What is its own bottleneck?



---

## Problems and fixes

Anything that went wrong and what resolved it. Paste the actual error message. If nothing
went wrong, say so.



---

## Use of generative AI

If you used a generative AI tool, include the acknowledgment statement from the syllabus and
say specifically what you used it for. If you did not use one, say so.


