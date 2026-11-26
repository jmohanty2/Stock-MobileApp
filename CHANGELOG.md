# Changelog

All notable changes to Personal Stock Tracker are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Android project (Java 17, minSdk 26, targetSdk 37) with MVVM packages: `ui`, `data`, `domain`.
- Room database schema: stocks, holdings, price alerts and price history, with DAOs.
- Shared domain types: `Resource<T>`, `ErrorType`, `AlertType`, `Direction`, `Trend`, `Volatility`, and the `StockQuote`, `Holding`, `PortfolioSummary` and `PriceAlert` models.
- `InputValidator` for shares, buy price and alert values; shows "Invalid Value" on bad input (FR-P8, FR-D7).
- Gain/loss formulas in `Holding` and `PortfolioSummary` (FR-P4, FR-P5).
- `IndicatorCalculator`: 5- and 30-day change, 20-day average, 30-day high/low, volatility and slow-mover check (FR-D2, FR-A5).
- Internet, network-state and notification permissions; HTTPS-only traffic.
- API keys read from `local.properties` into `BuildConfig`.
- CI on GitHub Actions: lint, unit tests, coverage report and debug APK on every pull request.
- Release signing configuration and a tag-triggered workflow that publishes a signed APK and AAB to GitHub Releases.
- Contributing guide, commit message template, pull request and issue templates, CODEOWNERS.

### Planned for 1.0.0
- Watchlist: search and add US tickers, live price and daily change with ▲/▼ trend, pull-to-refresh, remove with Undo (FR-W1–W12).
- Portfolio: holdings with shares and buy price, total value, today's and total gain/loss (FR-P1–P9).
- Stock Details: quote, indicators and 30-day chart (FR-D1–D4).
- Price alerts by percent or target price, with push notifications (FR-D5–D8, FR-A1–A7).
- Slow-moving stock reminders with editable thresholds (FR-A5).

### Known limitations
- US-listed stocks only; prices in USD.
- Free market-data plans: quotes may be delayed up to 15 minutes; price history refreshes at most once a day per stock.
- At most 50 tracked stocks.
- Not a trading app and not financial advice.