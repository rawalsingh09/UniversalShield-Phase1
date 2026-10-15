# Trained models (optional)

This folder is empty in the repository on purpose. UniversalShield works fully without a model;
the About page then shows the ML detector as **Unavailable**.

Create models from the bundled synthetic datasets (Windows):

```bat
mvnw.cmd -q compile exec:java -Dexec.args="--task message --data data/samples/messages_synthetic.csv --out models/message"
mvnw.cmd -q exec:java -Dexec.args="--task url --data data/samples/urls_synthetic.csv --out models/url"
```

Each run writes `model.json` (word counts only, no executable code), `metadata.json`
(version, dataset hash, split seed, metrics, baselines) and `evaluation.md`.
Restart the application to load them. See docs/ML.md.
