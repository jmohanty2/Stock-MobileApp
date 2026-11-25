# Personal Stock Tracker

A clean, beginner-friendly Android app to follow the stocks you own or watch: live prices, simple indicators, and push alerts when a price moves. It does not place trades.

**Course:** <course name and number> · **Team:** <Name (A)>, <Name (B)>, <Name (C)>, <Name (D)>

## Features

- **Watchlist** — add/remove tickers, live price, daily change, ▲/▼ trend
- **Portfolio** — shares owned, total value, today's and total gain/loss
- **Stock Details** — simple indicators from recent history, 30-day chart, custom price alerts
- **Alerts** — push notifications on % moves or price targets; slow-mover reminders

## Tech stack

Java 17 · Android Studio · minSdk 26 / targetSdk 35 · MVVM (ViewModel + LiveData) · Room · Retrofit + OkHttp + Gson · WorkManager · MPAndroidChart · Finnhub and Alpha Vantage APIs

## Getting started

1. Clone: **File → New → Project from Version Control…** in Android Studio.
2. Get free API keys from [Finnhub](https://finnhub.io) and [Alpha Vantage](https://www.alphavantage.co/support/#api-key).
3. Add them to `local.properties` in the project root (never commit this file):
   ```properties
   FINNHUB_API_KEY=your_key
   ALPHA_VANTAGE_API_KEY=your_key
   ```
4. Run ▶ on an emulator or device (Android 8.0+).

## Install the release

Download the latest `PersonalStockTracker-vX.Y.Z.apk` from **Releases**, open it on your phone and allow *Install unknown apps* when asked.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branches, commit messages and pull requests.

