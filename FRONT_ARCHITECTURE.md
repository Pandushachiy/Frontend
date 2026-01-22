# Frontend Architecture (FeyBerry)

## Overview
- Platform: Android (Jetpack Compose + Material3)
- Architecture: MVVM + Repository + Hilt DI
- Networking: Retrofit + OkHttp + Kotlinx Serialization
- Persistence: Room (messages, conversations, documents, health metrics)

## Project Structure
```
app/src/main/kotlin/com/health/companion
├─ App.kt
├─ MainActivity.kt
├─ data
│  ├─ local
│  │  ├─ dao
│  │  │  ├─ ChatMessageDao.kt
│  │  │  ├─ ConversationDao.kt
│  │  │  ├─ DocumentDao.kt
│  │  │  ├─ HealthMetricDao.kt
│  │  │  └─ MoodEntryDao.kt
│  │  └─ database
│  │     ├─ Converters.kt
│  │     ├─ Entities.kt
│  │     └─ HealthCompanionDatabase.kt
│  ├─ remote
│  │  ├─ api
│  │  │  ├─ AuthApi.kt
│  │  │  ├─ ChatApi.kt
│  │  │  ├─ DashboardApi.kt
│  │  │  ├─ DocumentApi.kt
│  │  │  ├─ HealthApi.kt
│  │  │  ├─ ProfileApi.kt
│  │  │  └─ VoiceApi.kt
│  │  └─ TokenAuthenticator.kt
│  └─ repositories
│     ├─ AuthRepository.kt
│     ├─ ChatRepository.kt
│     ├─ DashboardRepository.kt
│     ├─ DocumentRepository.kt
│     ├─ HealthRepository.kt
│     ├─ ProfileRepository.kt
│     └─ VoiceRepository.kt
├─ di
│  ├─ AppModule.kt
│  ├─ DatabaseModule.kt
│  ├─ MLModule.kt
│  ├─ NetworkModule.kt
│  └─ RepositoryModule.kt
├─ ml
│  ├─ camera/CameraManager.kt
│  └─ voice/VoiceInputManager.kt
├─ presentation
│  ├─ components
│  │  ├─ GlassMorphismBox.kt
│  │  └─ GlassMorphismCard.kt
│  ├─ navigation/NavGraph.kt
│  ├─ screens
│  │  ├─ auth (LoginScreen, RegisterScreen, AuthViewModel)
│  │  ├─ chat (ChatScreen, ChatViewModel)
│  │  ├─ dashboard (DashboardScreen, DashboardViewModel)
│  │  ├─ documents (DocumentsScreen, DocumentsViewModel)
│  │  ├─ health (HealthScreen, HealthViewModel)
│  │  ├─ mood (MoodScreen, MoodViewModel)
│  │  ├─ profile (ProfileScreen, ProfileViewModel)
│  │  └─ settings (SettingsScreen, SettingsViewModel)
│  └─ theme (Theme.kt, Typography.kt)
├─ services
│  ├─ HealthCompanionMessagingService.kt
│  └─ WebSocketManager.kt
└─ utils
   ├─ CrashLogger.kt
   ├─ TokenManager.kt
   └─ VoiceEventLogger.kt
```

## Navigation
- `NavGraph.kt` defines auth flow and main flow with bottom navigation.
- Primary routes: `dashboard`, `chat`, `documents`, `settings`, `profile`.

## Data Flow
- UI -> ViewModel -> Repository -> API/DB
- Repositories return `Result<T>` and persist to Room where needed.
- `TokenAuthenticator` handles automatic refresh on 401.

## Key Screens
- **Dashboard**: data from `GET /api/v1/dashboard`, widgets rendered by type.
- **Chat**: messages persisted in Room; supports multi-chat; voice uses server STT.
- **Documents**: list-first layout, local renames, swipe delete, polling for status.
- **Profile**: profile + knowledge graph + routing stats.

## UI/UX Conventions
- Dark theme with material3 color scheme.
- Compact bottom navigation for one-hand use.
- Input bars anchored above IME, lists scroll to bottom on new messages.

## Build Config
- Base URL: `BuildConfig.API_BASE_URL`
- WebSocket: `BuildConfig.WS_URL`

## Notes
- App name: `FeyBerry` (from `strings.xml`).
