# Demo guide (for presenting to a teacher or panel)

A 10-12 minute script. Everything runs offline on one laptop after the first build.

## Before the presentation (the day before)

1. Run `scripts\build.cmd` once. It must end with `BUILD SUCCESS`. This also downloads every
   dependency, so the demo itself needs no internet.
2. Optional: run `scripts\train-models.cmd` so the About page shows the ML detector as
   available.
3. Start fresh: delete `data\devdb.*`, then run `java -jar target\universalshield.jar` and wait for
   `Started UniversalShieldApplication`.
4. Open http://localhost:8080 in Chrome or Edge, zoom to 110-125 % for the projector, and keep
   this guide open on a second screen or on paper.
5. Have `data\samples\sample_permission_report.txt` ready for the upload step.

Demo account: **`demo@universalshield.local` / `ShieldDemo2026`** (dev profile only). It comes
with 7 seeded scans marked **Demo**.

## Script

### 1. The problem and the idea (1 min, landing page)

"Fraud reaches people through five channels: messages, links, payments, calls and apps.
UniversalShield checks all five with one explainable engine. It tells you **why** something
looks risky and what to do, and it is honest about what it cannot know."

Point at the landing page box "What the score means": a high score is not proof of fraud, and a low score is never a guarantee of safety.

### 2. Sign in and the dashboard (1 min)

Sign in with the demo account. Show the totals and the charts: risk-level distribution and
scans by category. "These charts are computed from this user's real stored scans. With no scans
the page shows an empty state, not invented numbers."

### 3. Message scan: the strongest example (2 min)

Scanner → **Message**, channel SMS, paste:

```
Your bank account will be BLOCKED today. Share the OTP sent to you now to keep it active.
```

Press **Analyze**. Expected: **87, CRITICAL, evidence STRONG** (OTP request, threat, bank impersonation). On the result page explain:

- the **score** and **level**,
- **evidence strength**, separate from the score: "how much independent support the score has",
- each **indicator** with its weight and group: "signs in the same group count once, so there is no double counting",
- **recommendations** ("Never share an OTP..."),
- the **disclaimers** and the ruleset and detector **versions**: "every result can be traced to the exact rules that produced it".

Click **Export HTML report** and open the downloaded file to show the exported report.

### 4. Contrast: an ordinary message (30 s)

```
Hi, are we still meeting at the library at 4pm to finish the database assignment?
```

It scores **0, LOW, evidence LIMITED**. Point out the disclaimer: "A low score does not guarantee safety."

### 5. URL scan: look-alike domain (1 min)

```
http://paypa1-secure-login.top/verify
```

Expected: **87, CRITICAL**. Explain the look-alike (`paypa1` → `paypal`), the abused TLD, no HTTPS and the lure words. Stress
that **the link is never opened**: the analysis is structural, so the scanner cannot be
infected or tracked by the site.

### 6. Transaction scan: the "PIN to receive money" scam (1 min)

Amount 15000 INR, recipient `buyer-olx-7781@upi`, new recipient = yes, method UPI, requested by
other party = yes, description:

```
Buyer says I must enter my UPI PIN to receive the payment for my old phone.
```

Expected: **95, CRITICAL** (the cap). "You never need a PIN to **receive** money. This is one of the most common UPI scams in India."

### 7. Call scan: "digital arrest" (1 min)

Claimed identity `Cyber crime police officer`, description:

```
Caller said a parcel in my name had illegal items, I am under digital arrest, must stay on video call, not tell anyone and transfer money to verify.
```

Expected: **94, CRITICAL**. Show the threat ("digital arrest"), government-impersonation, secrecy and payment indicators, and the note that the
system never records calls.

### 8. App permissions (1 min)

Scanner → **App permissions** → upload `data\samples\sample_permission_report.txt` (or use
**Load synthetic example**). "What does the app say it does?" **Flashlight**, "Installed from" **APK / link / unknown source**. Show the
banking-trojan combination (SMS + accessibility/overlay). Expected: **95, CRITICAL**. Also point out "permission normal for this kind of
app" being down-weighted.

### 9. History and privacy (1 min)

History: filter by **CRITICAL**, search for `OTP`, open one, delete one (confirmation dialog).
Settings: retention days, the "store input excerpts" switch, delete all history, delete
account. "OTPs, card numbers and long digit runs are masked before anything is stored."

### 10. Methodology and honesty (1 min, About page)

Show the detector list with versions, the formula, the thresholds and the ML status. Say plainly:

- weights are expert-assigned and **not calibrated** on real data,
- the optional ML model was trained on **synthetic** data and on that data is only as good as the rules,
- no external reputation service is connected, so scores are capped at 95.

### 11. Engineering (1 min, optional)

Show `/swagger-ui.html` (the API contract), run `scripts\run-tests.cmd` if time allows (real
test count is in [TESTING.md](TESTING.md)), and show the diagrams in
[REQUEST_FLOW.md](REQUEST_FLOW.md).

## Likely questions and honest answers

| Question | Answer |
|---|---|
| How accurate is it? | "We have no real-world accuracy figure, and we deliberately do not invent one. On our small synthetic test set the rules and the model both score F1 around 0.92-0.95, which mostly shows the pipeline works. Measuring real accuracy needs a labelled real dataset." |
| Why not just use ML? | "Rules are explainable, need no training data, and work offline. The model is optional supporting evidence with a capped weight." |
| Why is 100 never shown? | "Without verified external evidence we cap at 95. We only see what the user typed." |
| What if the attacker changes the wording? | "Then the rules can miss it. That's a real limitation, and the reason a LOW score always comes with a warning." |
| Is user data safe? | "Passwords are BCrypt-hashed, sensitive numbers are masked, identifiers are stored only as keyed hashes, every query is scoped to the owner, and users can delete everything." |
| Can it check if a URL is live or malicious? | "Not in this version, on purpose. Fetching attacker URLs from a server is a security risk (SSRF). SECURITY.md lists the safeguards that would be required." |

## If something goes wrong during the demo

- Page blank or stuck: `Ctrl+F5`.
- "Port 8080 already in use": another copy is running. Close it, or run on another port with `set PORT=9090`.
- Login says invalid: the dev database may be from an older run. Stop, delete `data\devdb.*`, start again.
- More fixes: [DEPLOYMENT.md, section 11](DEPLOYMENT.md#11-windows-troubleshooting).
