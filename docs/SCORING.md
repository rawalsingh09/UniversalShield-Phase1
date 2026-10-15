# Risk scoring

This is the exact method the code uses (`engine/RiskScorer.java`, `engine/RiskEngine.java`).
Ruleset version: **2026.10-1** (`RiskEngine.RULESET_VERSION`). Every stored scan records this
version and each detector's version, so a result can always be traced back to the rules that
produced it.

> Honest status: the weights below are **expert-assigned starting values**, chosen by reading
> public anti-fraud guidance. They have **not** been calibrated against a labelled real-world
> dataset. The score is a ranking of how many and how serious the warning signs are. It is not
> a probability that fraud is happening.

## 1. Vocabulary

| Term | Meaning | Where |
|---|---|---|
| Indicator | One warning sign found in the input: code, group, title, explanation, weight in (0, 1], detector, source, masked evidence | `engine/Indicator.java` |
| Group | A family of correlated signs (e.g. `URGENCY`, `OTP_REQUEST`, `DECEPTIVE_DOMAIN`). Groups from embedded links are namespaced `url:`, the ML signal is `ml:MODEL` | indicator `group` |
| Weight | How serious one sign is on its own, 0-1 | detector code |
| Risk score | 0-100 combination of the group weights | `RiskScorer.score` |
| Risk level | LOW / MEDIUM / HIGH / CRITICAL from the score and thresholds | `RiskLevel.fromScore` |
| Evidence strength | LIMITED / MODERATE / STRONG: how much independent support the score has. Kept separate from the score on purpose | `RiskScorer.evidence` |
| Source | `LOCAL_HEURISTIC`, `TRAINED_MODEL` or `VERIFIED_EXTERNAL` | `ResultSource` |

## 2. The formula

Let the indicators of a scan be grouped by `group`. For each group *k* take only the strongest
weight:

    g_k = max(weight of indicators in group k)

Combine groups with a noisy-OR:

    raw   = 100 × (1 − Π_k (1 − g_k))
    score = round(raw + 1e-9), clamped to [0, 100]
    score = min(score, 95)    unless at least one source is VERIFIED_EXTERNAL

The `1e-9` only fixes floating-point cases such as 77.4999999 so that 77.5 rounds to 78.

Why this shape:

- **No double counting.** "Act now" and "within 24 hours" are both `URGENCY`, so together they
  count once. Correlated tactics share a group by design.
- **Bounded.** The product is always between 0 and 1, so the score never leaves 0-100.
- **Diminishing returns.** Each new independent group adds `g × (what is left)`. Many weak signs
  cannot outweigh a few strong ones by much.
- **Order-independent.** The result does not depend on which detector ran first.
- **The 95 cap.** Local heuristics only see what the user typed. Without verified external
  evidence (for example a confirmed malicious-domain feed) the system refuses to claim
  near-certainty, so 96-100 is reserved. No external provider is configured in this version, so
  in practice scores never exceed 95.

Known limitation: noisy-OR assumes groups are independent. They are only approximately
independent (an impersonation message often also contains urgency). That is why group weights
are deliberately moderate.

## 3. Risk levels

| Level | Default score range | Config property | Env var |
|---|---|---|---|
| LOW | 0-29 | | |
| MEDIUM | 30-59 | `universalshield.risk.thresholds.medium` | `RISK_THRESHOLD_MEDIUM` |
| HIGH | 60-84 | `universalshield.risk.thresholds.high` | `RISK_THRESHOLD_HIGH` |
| CRITICAL | 85-100 | `universalshield.risk.thresholds.critical` | `RISK_THRESHOLD_CRITICAL` |

`RiskThresholds` rejects invalid configurations at startup (they must satisfy
`0 < medium < high < critical <= 100`).

## 4. Evidence strength

Computed from groups whose strongest weight is at least **0.15** ("independent groups"):

| Condition (checked in this order) | Evidence |
|---|---|
| Any detector reported `inputSufficient = false` | LIMITED |
| 3 or more independent groups | STRONG |
| 2 independent groups, or 1 group with weight ≥ 0.5 | MODERATE |
| otherwise | LIMITED |

Input is "insufficient" when a message has fewer than 4 words, a call description fewer than 5
words, a URL cannot be parsed, a transaction has only an amount and no context, or none of an
app's permissions are recognised. A LIMITED result always carries an extra disclaimer.

## 5. Weights

### Shared text signals (`detectors/common/TextSignals.java`)

Used by the message, call and transaction detectors. Negated phrases such as "never share your
OTP" do not trigger the OTP, credential and PIN signals.

| Code | Group | Weight |
|---|---|---|
| OTP_REQUEST | OTP_REQUEST | 0.70 |
| PIN_CVV_REQUEST | CREDENTIAL_REQUEST | 0.70 |
| PIN_TO_RECEIVE | RECEIVE_MONEY_MYTH | 0.70 |
| REMOTE_ACCESS_REQUEST | REMOTE_ACCESS | 0.65 |
| CREDENTIAL_REQUEST | CREDENTIAL_REQUEST | 0.55 |
| KYC_PRESSURE | CREDENTIAL_REQUEST | 0.50 |
| PRIZE_LURE | PRIZE_LURE | 0.45 |
| THREAT_LANGUAGE | THREAT | 0.45 |
| UNUSUAL_PAYMENT_METHOD | PAYMENT_METHOD | 0.45 |
| PAYMENT_DEMAND | PAYMENT_REQUEST | 0.40 |
| SECRECY_REQUEST | SECRECY | 0.40 |
| TOO_GOOD_TO_BE_TRUE | TOO_GOOD | 0.40 |
| FAKE_SUPPORT | FAKE_SUPPORT | 0.35 |
| URGENCY_LANGUAGE | URGENCY | 0.25 |
| LINK_CALL_TO_ACTION | LINK_CTA | 0.25 |
| ORG_IMPERSONATION | IMPERSONATION | 0.20 |

Overrides: the **call** detector raises signals that are more alarming when spoken (OTP 0.75,
remote access 0.70, threats 0.55, gift cards/crypto 0.50, payment demand 0.45, secrecy 0.45,
fake support 0.40, impersonation 0.30). The **transaction** detector lowers urgency to 0.20 and
impersonation to 0.15 because a payment description is usually written by the user.

### Message-specific (`MessageFraudDetector`)

| Code | Group | Weight |
|---|---|---|
| MSG_FREEMAIL_ORG_CLAIM (free-mail sender claims to be an organisation) | IMPERSONATION | 0.45 |
| MSG_KNOWN_RISKY_SENDER (rated HIGH/CRITICAL in your earlier scans) | KNOWN_RISKY | 0.45 |
| MSG_SENDER_BODY_MISMATCH | IMPERSONATION | 0.35 |

Links inside messages, calls and transaction descriptions are also analysed by the URL
detector, with groups prefixed `url:` so they stay separate from text groups.

### URL (`UrlRiskDetector`, never opens the link)

| Code | Group | Weight |
|---|---|---|
| URL_SCRIPT_SCHEME (`javascript:`, `data:`) | OBFUSCATION | 0.90 |
| URL_REPUTATION_MALICIOUS (only with a verified provider) | REPUTATION | 0.90 |
| URL_USERINFO_TRICK (`https://bank.com@evil.test`) | OBFUSCATION | 0.60 |
| URL_TYPOSQUAT (edit distance ≤ 1 for brands under 7 chars, ≤ 2 otherwise, after rn→m, vv→w, leetspeak normalisation) | DECEPTIVE_DOMAIN | 0.60 |
| URL_BRAND_MISUSE (brand in an unrelated registrable domain) | DECEPTIVE_DOMAIN | 0.55 |
| URL_EXECUTABLE_DOWNLOAD (.apk, .exe, ...) | DOWNLOAD | 0.55 |
| URL_FILE_SCHEME / URL_ENCODED_HOST / URL_REPUTATION_SUSPICIOUS | OBFUSCATION / OBFUSCATION / REPUTATION | 0.50 |
| URL_IP_HOST / URL_NON_ASCII_HOST / URL_PREVIOUSLY_HIGH_RISK | HOST_FORM / DECEPTIVE_DOMAIN / KNOWN_RISKY | 0.45 |
| URL_PUNYCODE | DECEPTIVE_DOMAIN | 0.40 |
| URL_BRAND_IN_PATH | DECEPTIVE_DOMAIN | 0.35 |
| URL_MALFORMED, URL_OPEN_REDIRECT, URL_LURE_KEYWORDS (0.15-0.30 by count) | STRUCTURE, OBFUSCATION, LURE_KEYWORDS | 0.30 |
| URL_SHORTENER, URL_RISKY_TLD, URL_DEEP_SUBDOMAINS, URL_HEAVY_ENCODING | own groups | 0.25 |
| URL_NO_HTTPS, URL_UNUSUAL_SCHEME, URL_NONSTANDARD_PORT, URL_HYPHENATED_DOMAIN | TRANSPORT, STRUCTURE, HOST_FORM, STRUCTURE | 0.20 |
| URL_VERY_LONG | STRUCTURE | 0.10 |

### Transaction (`TransactionFraudDetector`)

| Code | Rule | Group | Weight |
|---|---|---|---|
| TX_AMOUNT_EXTREME | ≥ 10 × median of your last payments (needs ≥ 5 prior amounts) | AMOUNT_ANOMALY | 0.50 |
| TX_AMOUNT_UNUSUAL | ≥ 3 × median | AMOUNT_ANOMALY | 0.35 |
| TX_LARGE_AMOUNT | fewer than 5 prior amounts and amount ≥ `TRANSACTION_LARGE_AMOUNT` (default 10000) | AMOUNT_ANOMALY | 0.20 |
| TX_GIFT_CARD / TX_CRYPTO | payment method | PAYMENT_METHOD | 0.55 / 0.40 |
| TX_VELOCITY | ≥ 5 attempts in the last hour (≥ 3 gives 0.30) | VELOCITY | 0.50 |
| TX_KNOWN_RISKY | recipient rated HIGH/CRITICAL before | KNOWN_RISKY | 0.45 |
| TX_NEW_RECIPIENT | first payment to this recipient | RECIPIENT | 0.30 |
| TX_REQUESTED | the other party asked for the payment | PAYMENT_REQUEST | 0.30 |

### Call (`CallFraudDetector`)

| Code | Group | Weight |
|---|---|---|
| CALL_REPUTATION (only with a verified provider) | REPUTATION | 0.85 / 0.45 |
| CALL_GOV_CLAIM (police, tax, customs...) | IMPERSONATION | 0.55 |
| CALL_KNOWN_RISKY | KNOWN_RISKY | 0.45 |
| CALL_BANK_CLAIM | IMPERSONATION | 0.30 |
| CALL_SHORT_NUMBER | NUMBER_FORMAT | 0.10 |

### App permissions (`AppPermissionDetector`, `PermissionCatalog`)

Each recognised Android permission has a base weight (e.g. `BIND_ACCESSIBILITY_SERVICE` 0.60,
`BIND_DEVICE_ADMIN` 0.60, `READ_SMS` 0.55, `BIND_NOTIFICATION_LISTENER_SERVICE` 0.45,
`SYSTEM_ALERT_WINDOW` 0.40, `READ_CONTACTS` 0.25, `CAMERA` 0.15). If the permission is normal
for the declared app category (SMS for a messaging app, camera for a camera app) its weight is
multiplied by **0.25**. Combinations:

| Code | Rule | Weight |
|---|---|---|
| PERM_COMBO_BANKING_TROJAN | unjustified SMS access together with accessibility, overlay or notification access | 0.75 |
| PERM_COMBO_PREDATORY_LOAN | loan app reading contacts, SMS or call log | 0.55 |
| PERM_EXCESSIVE_SIMPLE_APP | flashlight, game or utility app with ≥ 3 unjustified sensitive permissions | 0.35 |
| APP_SIDELOADED | installed from an unknown source | 0.30 |
| PERM_EXCESSIVE | ≥ 5 unjustified sensitive permissions | 0.25 |

### Optional ML (`MlTextDetector`)

Only when a trained model exists and `p(fraud) ≥ 0.6`. Weight rises linearly from 0.10 at
p = 0.6 to a hard maximum of **0.35** at p = 1.0, in its own group `ml:MODEL`. The cap keeps the
model as supporting evidence: it can move a score, but it can never on its own produce HIGH.

## 6. Worked example (real output)

Message (SMS): *"Your bank account will be BLOCKED today. Share the OTP sent to you now to keep it active."*

Submitted to the running dev server on 2026-10-09; the API returned these indicators:

| Group | Indicator | g |
|---|---|---|
| OTP_REQUEST | OTP_REQUEST | 0.70 |
| THREAT | THREAT_LANGUAGE | 0.45 |
| IMPERSONATION | ORG_IMPERSONATION | 0.20 |

    1 − (0.30 × 0.55 × 0.80) = 1 − 0.132 = 0.868  →  score 87  →  CRITICAL

Three groups at or above 0.15 give **STRONG** evidence. The API returned exactly
`87 CRITICAL STRONG`. Note what did *not* fire: "today" and "now" alone are not matched as
`URGENCY` (the urgency patterns look for phrases like "within 24 hours", "immediately",
"act now"), which is a deliberate choice to reduce false positives on ordinary messages.

## 7. Recommendations

`RecommendationCatalog` maps each group (ignoring the `url:`/`ml:` prefix) to one concrete
action plus a reason, adds one action for the overall level (for CRITICAL: "Stop. Do not click, pay, install or share anything related to this."),
removes duplicates and returns the 6 highest-priority actions. Several privacy groups (contacts,
location, microphone...) share one "grant only when needed" recommendation.

## 8. How to calibrate (future work)

1. Collect a labelled set of real, consented, de-identified cases per category (at least a few
   hundred per class).
2. Run every case through `RiskEngine` and store the indicators, not only the score.
3. Fit per-indicator weights, for example with logistic regression on group indicator values,
   keeping weights in (0, 1]. Re-check that correlated signs still share groups.
4. Choose thresholds from the precision/recall curve for the false-positive rate you accept.
5. Bump `RULESET_VERSION`, record the dataset and metrics in this file, and keep the old
   version's results reproducible.

Until that is done, report results as "heuristic risk level", never as an accuracy figure.
