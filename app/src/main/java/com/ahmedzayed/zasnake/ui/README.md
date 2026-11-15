# Za Snake — PLAYFORM UI files

All Kotlin files in this directory tree intentionally use the same Kotlin package
`com.ahmedzayed.zasnake.ui`. The subdirectories organize code by role without
changing public signatures or the game's existing persistence and navigation.

## Where to edit

| Task | File |
| --- | --- |
| App startup, routing, Play Games and ad/consent UI hooks | `ZaSnakeApp.kt` |
| Arabic / English interface copy | `localization/Translations.kt` |
| UI colors and board theme palette | `theme/Palette.kt` |
| Home | `screens/HomeScreen.kt` |
| Game mode cards | `screens/ModeSelectScreen.kt` |
| Gameplay, board and touch controls | `screens/GameScreen.kt` |
| Pause, Game Over and revive | `components/GameDialogs.kt` |
| Shop/Style Lab | `screens/StyleLabScreen.kt` |
| True snake previews | `components/SnakePreviews.kt` |
| Daily rewards and daily challenge cards | `screens/DailyRewardsScreen.kt` |
| Achievement rewards and progress cards | `screens/AchievementsScreen.kt` |
| Player statistics | `screens/StatisticsScreen.kt` |
| Settings and account | `screens/SettingsScreen.kt` |
| Shared buttons and compact **Claim** button | `components/SharedUi.kt` |
| Theme samples and chooser cards | `components/ThemePreviews.kt` |
| Reward popup | `components/RewardPopOverlay.kt` |
| Shared page header and cloud status | `components/ScreenHeader.kt`, `components/CloudStatusLine.kt` |

## Rules when making UI-only changes

- Preserve `GameViewModel`, `SnakeEngine`, age/child policy, reward ledger, Billing, AdMob,
  Play Games and persistence behavior unless a separate change explicitly requires it.
- Keep PLAYFORM application chrome independent from the player's selected game-board theme
  and equipped snake colors/head/body.
- Use the shared `RewardClaimButton` for daily and achievement claim actions; it
  has a compact **36dp visual height**, while Material 3 handles the touch target.
- Never grant rewards in Compose state: call the existing `onClaimChallenge` or
  `onClaimReward` callback, and show the claimed icon only from persisted state.
- Use the existing snake renderer for preview cards (especially Crystal vs Smooth).
- Test Arabic RTL and English LTR, small displays, landscape and large font scale.

## Verification

Before merging a refactor branch into `main`, run in Android Studio:
`Build > Rebuild Project`, then test gameplay, shop actions, daily/achievement
claims, account sync, and child local-only behavior on a device or emulator.

The source split itself deliberately changes file organization, **not** gameplay rules.
