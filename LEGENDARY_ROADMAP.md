# Project Evolution: The Autonomous Wealth OS (Legendary Roadmap)

> **Architectural Decision Record & Retrospective Note:**
> The repository `hitmanM340/pennywiseai-tracker` previously diverged from `sarim2000/pennywiseai-tracker` into an independent root commit history (`git init` & standalone force push). Pull requests to upstream are no longer the objective.
> 
> **The New Mandate:** Liberation from upstream constraints. Rather than maintaining a downstream fork, this project is pivoting into an independent, uncompromising, ultra-powerful personal finance and wealth operating system.

---

## 1. Vision: From Simple SMS Tracker to Sovereign Wealth OS

Transform PennyWise into a privacy-first, fully local financial cockpit that outclasses commercial subscription apps (Copilot, Monarch Money, Wallet, YNAB) with zero tracking, zero subscription fees, and complete offline capability.

---

## 2. Master Feature Roadmap: The 5 Legendary Pillars

### Pillar 1: Multi-Modal Intelligent Ingestion
- **Beyond Basic SMS**:
  - Direct PDF/CSV Bank Statement Parser with multi-page table extraction and password caching.
  - Notification listener with AI regex inference (auto-learn parsing rules on unknown notifications).
  - Receipt OCR: On-device text recognition (ML Kit) extracting itemized lines, tax, and merchant logos.
  - UPI Intent & QR Scanner: Direct payment launch with pre-filled category/budget tracking.

### Pillar 2: Autonomous Local AI Agent (On-Device Intelligence)
- **Financial Co-Pilot**:
  - Run quantized local models (Qwen 2.5 1.5B / Gemma 2) via MediaPipe GenAI / LiteRT.
  - "Spend Simulator": Ask questions like *"Can I afford a $1,200 laptop this month without dipping into emergency savings?"* and receive mathematically grounded answers.
  - Subscription Hunter: Detect hidden creeping price increases and forgotten recurring memberships.
  - Smart Split: Bill-splitting with friends via localized QR codes or encrypted peer-to-peer links.

### Pillar 3: Total Net Worth & Wealth Portfolio Tracking
- **Unified Balance Sheet**:
  - Assets vs. Liabilities real-time gauge.
  - Debt Payoff Engine: Integrated Snowball vs. Avalanche strategies with payoff date countdowns.
  - Investment Tracking: Local tracking of mutual funds (NAV sync), stocks, gold, fixed deposits, and crypto with historical profit/loss curves.
  - Cash-Flow Runway: Forecast balance 30/60/90 days ahead based on recurring bills and salary cycles.

### Pillar 4: Sovereign, Zero-Knowledge Sync & Backup
- **True Ownership**:
  - Multi-device sync using self-hosted WebDAV, Nextcloud, Syncthing, or local Wi-Fi pairing.
  - Zero-knowledge end-to-end encryption (`PWENC1` AES-256-GCM) with client-side key derivation.
  - Instant export to Excel, SQLite, and JSON with schema validation.

### Pillar 5: Elite AMOLED "Financial Cockpit" UI/UX
- **Design & Interactivity**:
  - Material 3 Expressive + Bento Grid widgets for home dashboard.
  - Smooth 120Hz micro-interactions, rich haptic feedback for every transaction add.
  - Interactive drill-down charts (spending heatmaps, Sankey diagrams of income → expenses → savings).
  - Expanded Glance widgets suite with interactive quick-action buttons.
