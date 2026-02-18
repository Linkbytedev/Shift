# Shift - End-to-End Encrypted Messaging

![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-orange.svg)
![Security](https://img.shields.io/badge/Security-AES--256--GCM-blueviolet.svg)

Shift is a premium, privacy-focused messaging application for Android. Built with a "Security First" mindset, it features a stunning minimalist UI with glassmorphism effects and complete end-to-end encryption for all user communications.

> [!NOTE]
> This repository is a technical demonstration of the app's architecture and UI. It currently uses in-memory mock implementations for authentication and storage to allow for easy exploration without external dependencies.

## ✨ Key Features

### 🛡️ State-of-the-Art Security
- **End-to-End Encryption**: All messages are encrypted with AES-256-GCM on the device before transmission.
- **Diffie-Hellman Key Exchange**: Secure, hardware-backed key agreement between participants.
- **Secure Vault**: A password-protected, encrypted space for storing sensitive photos and media.
- **Screenshot Protection**: Optional security layer that prevents or notifies about screenshots in private chats.
- **Self-Destructing Messages**: Configurable timers for messages that automatically wipe content from both devices.

### 🎨 Premium UI/UX
- **Glassmorphism Design**: Elegant, translucent interfaces with smooth blurs and vibrant gradients.
- **Modern Animations**: Powered by Jetpack Compose for fluid, responsive transitions.
- **Dynamic Themes**: Supports system-wide Dark and Light modes with optimized OLED contrast.
- **Deep Immersion**: Full edge-to-edge support for a modern mobile experience.

### 💬 Rich Communication
- **Private Messaging**: One-on-one encrypted chat rooms.
- **Media Sharing**: Encrypted image sharing with "View Once" capabilities.
- **WebRTC Voice & Video**: Real-time high-quality calling support.
- **Story Sharing**: Share disappearing text-based updates with your friends.

## 📂 Project Structure & Navigation

The codebase is organized following Clean Architecture principles. Here is where you can find specific logic:

### 📱 Presentation Layer (`app/src/main/java/com/Linkbyte/Shift/presentation`)
Contains all UI components, Composables, and ViewModels.
- **`auth/`**: Login and Sign-up screens with premium animations.
- **`chat/`**: Main messaging interface and `ConversationViewModel`.
- **`stories/`**: Snapchat-like story posting and viewing logic.
- **`vault/`**: Secure, encrypted photo storage UI.
- **`call/`**: WebRTC-powered voice and video call interfaces.
- **`navigation/`**: Centralized routing logic using Compose Navigation.

### ⚙️ Domain & Data Layers
- **`domain/repository/`**: Abstractions for data operations.
- **`data/repository/`**: Implementations (Mocks for the demo) and JSON parsing logic.
- **`data/model/`**: Unified data models used across the app.

### 🔐 Security & Core (`app/src/main/java/com/Linkbyte/Shift/security`)
The heart of the app's privacy features.
- **`encryption/`**: AES-256-GCM implementation for text and binary data.
- **`keyexchange/`**: Diffie-Hellman key agreement logic.
- **`keystore/`**: Secure storage of session keys using Android Keystore.
- **`vault/`**: Separate encryption layer for the persistent Secure Vault.

### 🌐 Networking & Signaling
- **`webrtc/`**: WebRTC client configuration and signaling protocols.
- **`service/`**: Background services, including the `NotificationService`.

## 🏗️ Technical Architecture

The project follows **Clean Architecture** principles and **MVVM** pattern, ensuring a highly decoupled and testable codebase:

- **presentation**: Jetpack Compose UI, ViewModels, and UI-specific state handling.
- **domain**: Business logic, use cases, and repository interfaces.
- **data**: Repository implementations and local/mock data sources.
- **security**: Core cryptographic logic, KeyStore management, and DH key exchange.

## 🛠️ Stack
- **Kotlin**: 100% Coroutines & Flow
- **Jetpack Compose**: Modern Toolkit for native UI
- **Hilt**: Dependency Injection
- **Android Keystore**: Hardware-backed security
- **OkHttp**: Resilient networking
- **Coil**: Optimized image loading

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or later
- JDK 17+
- Android Device/Emulator (API 26+)

*Since this is a demo version, you can sign up with any email/username to explore the UI and features.*

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing
This is a demo/educational project. Feel free to fork and modify for your own learning!

---

**Developed with ❤️ by Linkbyte**
