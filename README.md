# Dungeon Crawler - Complete Android Project

A full-featured procedurally-generated dungeon crawler with MDP-based AI.

## Features
- **Procedural Generation**: BSP, Cellular Automata, Random Walk algorithms
- **MDP AI**: Enemies learn and adapt to player behavior
- **Real-time Gameplay**: Smooth Canvas rendering at 60 FPS
- **Narrative System**: Procedural story generation
- **Ads-Only Monetization**: Banner + Interstitial + Rewarded ads
- **100% Local**: No server dependencies, privacy-first

## Setup

1. Open in Android Studio
2. Replace AdMob App ID in `AndroidManifest.xml` with your real ID
3. Replace test ad unit IDs in `AdManager.kt` with your real IDs
4. Build and run!

## Project Structure

```
app/src/main/
├── java/com/yourpackage/dungeoncrawler/
│   ├── MainActivity.kt          # Main activity
│   ├── GameEngine.kt            # Core game logic
│   ├── GameState.kt             # Game state data
│   ├── PlayerStats.kt           # Player progression
│   ├── Item.kt                  # Items and inventory
│   ├── DungeonMap.kt            # Map representation
│   ├── MapGenerator.kt          # Procedural generation
│   ├── MDPModel.kt              # AI decision making
│   ├── NarrativeEngine.kt       # Story generation
│   ├── SettingsManager.kt       # User preferences
│   ├── GameLoop.kt              # Real-time game loop
│   ├── ads/AdManager.kt         # AdMob integration
│   ├── config/                  # Local configuration
│   ├── telemetry/               # Local analytics
│   ├── save/                    # Save system
│   └── ui/GameView.kt           # Canvas rendering
├── assets/
│   ├── config/balance.json      # Game balance
│   ├── content/enemies.json     # Enemy definitions
│   └── i18n/en.json             # Localization
└── res/                         # Android resources
```

## Privacy

This game is 100% local. The only network traffic is AdMob ad requests.
Players can export or delete their local analytics data anytime.

## License

MIT License
