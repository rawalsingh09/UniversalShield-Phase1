# Optional machine-learning module

UniversalShield works fully **without** ML. The five rule-based detectors are the core
product. The ML module is an optional, clearly labelled extra that shows a correct training
and evaluation workflow in plain Java. It uses no Python, no downloaded models and no paid API.

## 1. What it is

| Item | Value |
|---|---|
| Algorithm | Multinomial Naive Bayes, Laplace smoothing, written from scratch (`ml/NaiveBayesModel.java`) |
| Tasks | `message` (SMS/e-mail/chat text) and `url` |
| Features | Messages: lower-cased words of 2+ characters plus word bigrams. URLs: alphanumeric tokens plus coarse length, dot count, hyphen count and scheme buckets (`ml/Tokenizer.java`) |
| Model file | `models/<task>/model.json`: word counts and class priors only. Plain JSON, no executable code, so loading it cannot run anything |
| Metadata | `models/<task>/metadata.json`: model version, dataset file and SHA-256, row counts, split seed, test ratio, metrics, baselines, decision threshold, vocabulary size, creation time |
| Report | `models/<task>/evaluation.md`: human-readable metrics table and limitations |
| Runtime use | `MlTextDetector`, available only if the model file exists and `ML_ENABLED=true` |

## 2. Training is separate from prediction

- **Training** runs only from the command line (`ml/training/TrainingCli`). It is never
  triggered by a web request, and the web application never writes model files.
- **Prediction** happens in `MlTextDetector` inside the running app, from the JSON files loaded
  at startup by `MlModelService`. Restart the app after training.

## 3. Dataset format

CSV with a header row, UTF-8, RFC 4180 quoting:

```csv
id,label,text
m001,1,URGENT: Your bank account will be blocked today. Update KYC now at http://kyc-update-bank.top/login
m051,0,Your order has been shipped and will arrive on Friday.
```

`label` is `1` for fraud and `0` for legitimate. Bundled files (both **synthetic**, written for
this project, see [../data/README.md](../data/README.md)):

| File | Rows | Fraud / legitimate |
|---|---|---|
| `data/samples/messages_synthetic.csv` | 100 | 50 / 50 |
| `data/samples/urls_synthetic.csv` | 80 | 40 / 40 |

## 4. Commands (Windows)

```bat
scripts\train-models.cmd
```

or one task at a time, with options:

```bat
mvnw.cmd -q compile exec:java -Dexec.args="--task message --data data/samples/messages_synthetic.csv --out models/message --seed 42 --test-ratio 0.25"
mvnw.cmd -q exec:java -Dexec.args="--task url --data data/samples/urls_synthetic.csv --out models/url"
```

| Option | Default | Meaning |
|---|---|---|
| `--task` | required | `message` or `url` |
| `--data` | required | CSV path |
| `--out` | required | output folder |
| `--seed` | 42 | random seed of the split |
| `--test-ratio` | 0.25 | share of rows held out for testing |
| `--version` | `<task>-nb-<timestamp>` | model version string stored in metadata |

## 5. Preventing data leakage

1. **De-duplication before the split** (`DatasetSplitter`). Texts are normalised (lower case,
   collapsed whitespace) and exact duplicates are removed, so the same message cannot appear in
   both train and test. Texts that appear with **both** labels are removed as conflicts.
2. **Stratified, seeded split.** Each class is shuffled with `new Random(seed)` and split
   separately, so the test set has the same class balance and the split is reproducible.
3. **The vocabulary comes from the training rows only.** Test rows are never seen during training.
4. **The test set is used once** for the final metrics. No tuning is done on it. The decision
   threshold (0.5) is fixed in advance.
5. **The dataset hash is recorded**, so anyone can confirm which data produced a model.

## 6. Metrics and baselines

For the test set the CLI reports precision, recall, F1, false-positive rate (FPR) and the
confusion matrix (TP, FP, TN, FN) for:

- the Naive Bayes model at p ≥ 0.5,
- a **majority-class baseline** (always predict the more frequent class),
- a **heuristic baseline**: the project's own rule-based detector, predicting fraud when its score is ≥ 30.

### Latest results (real output, 2026-10-09, seed 42, test ratio 0.25)

Messages: 100 rows, 0 removed, train 74, test 26.

| Classifier | Precision | Recall | F1 | FPR | TP | FP | TN | FN |
|---|---|---|---|---|---|---|---|---|
| Naive Bayes (p≥0.5) | 0.923 | 0.923 | 0.923 | 0.077 | 12 | 1 | 12 | 1 |
| Majority-class baseline | 0.500 | 1.000 | 0.667 | 1.000 | 13 | 13 | 0 | 0 |
| Heuristic detector (score≥30) | 1.000 | 0.846 | 0.917 | 0.000 | 11 | 0 | 13 | 2 |

URLs: 80 rows, 0 removed, train 60, test 20.

| Classifier | Precision | Recall | F1 | FPR | TP | FP | TN | FN |
|---|---|---|---|---|---|---|---|---|
| Naive Bayes (p≥0.5) | 1.000 | 0.900 | 0.947 | 0.000 | 9 | 0 | 10 | 1 |
| Majority-class baseline | 0.500 | 1.000 | 0.667 | 1.000 | 10 | 10 | 0 | 0 |
| Heuristic detector (score≥30) | 1.000 | 0.900 | 0.947 | 0.000 | 9 | 0 | 10 | 1 |

### How to read these numbers honestly

- The test sets have only 26 and 20 rows. One more or fewer mistake moves F1 by several
  points, so differences of a few points are noise.
- The data is **synthetic** and was written by the same people who wrote the rules. Both the
  model and the rules are therefore likely to look better here than on real messages.
- On this data the model is **about as good as the rule-based detector, not better**. That is
  the expected result, and it is the main reason the rules stay the primary detector and the
  model only adds supporting evidence.
- **None of these figures is a real-world accuracy claim**, and they must not be presented as
  one. The application shows this caveat next to every ML signal.

## 7. How the model affects a score

`MlTextDetector` reports an indicator only when `p(fraud) ≥ 0.6`. Its weight rises from 0.10 at
p = 0.6 to at most **0.35** at p = 1.0, in its own group `ml:MODEL`, with source
`TRAINED_MODEL`. Below 0.6 it only adds a note with the probability. On its own, the strongest
possible model signal gives a score of 35 (MEDIUM). So the model can strengthen a case made by
the rules, but it can never produce a HIGH or CRITICAL result by itself, and it never lowers a
score. See [SCORING.md](SCORING.md).

Known overlap: the model reads the same text the rules read, so its signal is partly
correlated with rule indicators. The 0.35 cap limits the resulting double counting.

## 8. Limitations and next steps

- Naive Bayes treats words as independent and is easily fooled by paraphrasing.
- English only; transliterated or mixed-language messages are not handled well.
- No probability calibration (e.g. Platt scaling): `p` is a model score, not a true probability.
- To make this meaningful, train on a real, licensed, de-identified dataset (for example a
  public SMS spam corpus, respecting its licence), keep a separate validation set for tuning,
  report confidence intervals, and re-run the comparison against the rules.
