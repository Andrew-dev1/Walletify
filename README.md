# Walletify — Personal Finance & Banking Insights App (Android)
A modern personal finance management app built with Jetpack Compose and Material 3. The app connects to bank accounts via the Plaid API to provide users with real-time transaction data, budgeting tools, and spending analytics in a clean, intuitive UI.

## ⚠️ Disclaimer

This project is for **educational and portfolio purposes only**. It is not a production banking app and does not handle real money transfers.

## Features

### 🔐 Authentication
* Login and registration flow
* Session persistence
* Scalable for future multi-user and shared accounts

### 📊 Dashboard
* Monthly spending overview
* Category-based breakdowns
* Onboarding "quick setup" bar for first-time users
* Visual summaries of spending habits

### 💸 Transactions
* Chronological transaction list (most recent first)
* Monthly section headers
* Category-based filtering
* Designed for future tagging and advanced filters

### 🎯 Savings & Budgeting
* Create monthly budgets by category
* Track budget progress visually
* Local notifications when nearing limits
* Extensible structure for future savings goals

### 👤 Profile & Settings
* Update account details (email, password)
* Account-sharing groundwork (families / couples)
* Secure handling of user preferences

### 📈 Financial Insights (Planned)
* Subscription detection
* Spending trend analysis
* Suggestions for potential savings
* Monthly spending comparisons

## 🛠️ Tech Stack

| Category | Technology |
|----------|---------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| Navigation | Type-safe Serializable routes |
| State Management | StateFlow |
| API Integration | Plaid API (Sandbox) |
| Dependency Injection | Hilt  |
| Notifications | Android Notifications |
| Version Control | Git + GitHub |

## 🧱 Architecture Overview

The app follows clean separation of concerns:
```
UI (Compose Screens) → ViewModel (StateFlow, UI State) → Repository (Business & Data Logic) → Data Sources (Plaid API, Local Cache)
```

### Key Principles
* UI is stateless
* ViewModels expose immutable UI state
* Repositories act as a single source of truth
* No API or security logic inside the UI layer

## 🗂️ Project Structure
```kotlin;
app/
├── data/
│   ├── model/
│   ├── repository/
│   └── remote/
├── domain/
│   └── analytics/
├── ui/
│   ├── auth/
│   ├── dashboard/
│   ├── transactions/
│   ├── savings/
│   └── profile/
├── navigation/
├── di/
└── MainActivity.kt
```

Each screen:
* Lives in its own file
* Uses a ViewModel only when necessary
* Is navigated using serializable route keys

## 🔐 Security Notes

* No API keys or secrets are stored in the app
* Plaid is used through Sandbox / mocked flows
* Tokens are abstracted behind repositories
* Sensitive files are excluded via `.gitignore`

## 🚀 Getting Started

### Prerequisites
* Android Studio Hedgehog or newer
* Kotlin 1.9+
* Gradle 8+
* Plaid Sandbox account (optional for mock data)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/Andrew-dev1/Walletify.git
cd Walletify
```

2. Open the project in Android Studio, sync Gradle, and run on an emulator or device.

## 🧪 Development Status

* ✅ Navigation & UI scaffolding
* ✅ Auth flow 
* ✅ Dashboard & transaction UI
* ✅ Plaid integration
* 🚧 Budget alerts
* 🚧 Analytics engine
* 🚧 AI chatbot

## 🧭 Roadmap

* Real backend token exchange
* Offline caching (Room)
* Multi-user account sharing
* Subscription auto-detection
* Export reports (CSV / PDF)
* Dark mode polish

## 📚 Learning Goals

This project was built to demonstrate:
* Advanced Jetpack Compose patterns
* Clean Android architecture
* Fintech data modeling
* Secure API integration concepts
* Scalable navigation and state handling

## 📜 License

MIT License. Feel free to fork, learn from, and build upon this project.

---

**Built using Kotlin and Jetpack Compose**