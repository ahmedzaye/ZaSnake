package com.ahmedzayed.zasnake.ui

import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.ahmedzayed.zasnake.game.GameTheme

internal data class ZaPalette(
    val bg: Color,
    val panel: Color,
    val panel2: Color,
    val accent: Color,
    val muted: Color,
    val gold: Color,
    val blue: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val border: Color,
    val divider: Color,
    val selectedPanel: Color,
    val chipBackground: Color,
    val buttonText: Color,
    val boardBackground: Color,
    val boardGrid: Color,
    val boardBorder: Color,
    val wallBorder: Color,
    val openBorder: Color,
    val joystickGlow: Color,
    val joystickGlowClear: Color,
    val joystickRing: Color,
    val joystickInner: Color,
    val joystickKnob: Color,
    val joystickHighlight: Color,
    val joystickHint: Color,
    val overlay: Color,
    val homeTop: Color,
    val homeBottom: Color,
    val scoreLabel: Color,
    val statsTile: Color
)

internal fun paletteFor(theme: GameTheme): ZaPalette = when (theme) {
    GameTheme.NIGHT -> ZaPalette(
        bg = Color(0xFF050A0E), panel = Color(0xFF0A1520), panel2 = Color(0xFF0C1914),
        accent = Color(0xFF00FF88), muted = Color(0xFF91A99D), gold = Color(0xFFFFD34E), blue = Color(0xFF9DC9FF),
        primaryText = Color(0xFFF4FAF6), secondaryText = Color(0xFFCCDAD1), border = Color(0xFF20392D), divider = Color(0xFF23392D),
        selectedPanel = Color(0xFF10251D), chipBackground = Color(0xFF101F1A), buttonText = Color(0xFF042014),
        boardBackground = Color(0xFF060D12), boardGrid = Color(0x1C00FF88), boardBorder = Color(0xFF1E6544),
        wallBorder = Color(0x99FF3366), openBorder = Color(0x5500CFFF),
        joystickGlow = Color(0x0D00FF88), joystickGlowClear = Color(0x0000FF88), joystickRing = Color(0x2200FF88),
        joystickInner = Color(0x1400FF88), joystickKnob = Color(0xFF00FF88), joystickHighlight = Color(0xFFA7FFD1),
        joystickHint = Color(0xFF426E57), overlay = Color(0xD9050A0E), homeTop = Color(0xFF07120F), homeBottom = Color(0xFF061117),
        scoreLabel = Color(0xFF8DAF9B), statsTile = Color(0xFF10251D)
    )
    GameTheme.LIGHT -> ZaPalette(
        bg = Color(0xFFF3F7F4), panel = Color(0xFFFFFFFF), panel2 = Color(0xFFEAF1EC),
        accent = Color(0xFF087F4B), muted = Color(0xFF536B5E), gold = Color(0xFF946000), blue = Color(0xFF245DB4),
        primaryText = Color(0xFF17251D), secondaryText = Color(0xFF42564A), border = Color(0xFFCCDAD1), divider = Color(0xFFD4E1D9),
        selectedPanel = Color(0xFFDFF4E8), chipBackground = Color(0xFFEEF5F0), buttonText = Color(0xFFFFFFFF),
        boardBackground = Color(0xFFFBFDFC), boardGrid = Color(0x1F087F4B), boardBorder = Color(0xFF87AA95),
        wallBorder = Color(0xFFC84654), openBorder = Color(0xFF4C8DB6),
        joystickGlow = Color(0x22087F4B), joystickGlowClear = Color(0x00087F4B), joystickRing = Color(0x33087F4B),
        joystickInner = Color(0x1F087F4B), joystickKnob = Color(0xFF087F4B), joystickHighlight = Color(0xFFB6EFD0),
        joystickHint = Color(0xFF547965), overlay = Color(0xB8F3F7F4), homeTop = Color(0xFFE6F2EA), homeBottom = Color(0xFFF8FBF9),
        scoreLabel = Color(0xFF5C7465), statsTile = Color(0xFFE0F1E6)
    )
    GameTheme.EMERALD -> ZaPalette(
        bg = Color(0xFF04150F), panel = Color(0xFF09291F), panel2 = Color(0xFF0D3628),
        accent = Color(0xFF43E5A0), muted = Color(0xFF9DCBBA), gold = Color(0xFFFFD166), blue = Color(0xFF9DE0FF),
        primaryText = Color(0xFFF1FFF7), secondaryText = Color(0xFFC5E2D5), border = Color(0xFF205A43), divider = Color(0xFF1A4B37),
        selectedPanel = Color(0xFF10422E), chipBackground = Color(0xFF0F3929), buttonText = Color(0xFF05261A),
        boardBackground = Color(0xFF061B13), boardGrid = Color(0x2543E5A0), boardBorder = Color(0xFF2B9268),
        wallBorder = Color(0xFFFF6175), openBorder = Color(0xFF559DD5),
        joystickGlow = Color(0x1943E5A0), joystickGlowClear = Color(0x0043E5A0), joystickRing = Color(0x3343E5A0),
        joystickInner = Color(0x1F43E5A0), joystickKnob = Color(0xFF43E5A0), joystickHighlight = Color(0xFFC5FFE5),
        joystickHint = Color(0xFF63A98A), overlay = Color(0xD904150F), homeTop = Color(0xFF08271C), homeBottom = Color(0xFF04150F),
        scoreLabel = Color(0xFF9BC6B3), statsTile = Color(0xFF10422E)
    )
    GameTheme.OCEAN -> ZaPalette(
        bg = Color(0xFF061627), panel = Color(0xFF0D2A42), panel2 = Color(0xFF123D58),
        accent = Color(0xFF41DDF2), muted = Color(0xFF93BFD4), gold = Color(0xFFFFD277), blue = Color(0xFF8AB8FF),
        primaryText = Color(0xFFF0FBFF), secondaryText = Color(0xFFCBEBF4), border = Color(0xFF26516A), divider = Color(0xFF224A60),
        selectedPanel = Color(0xFF154562), chipBackground = Color(0xFF15364B), buttonText = Color(0xFF06243B),
        boardBackground = Color(0xFF061D33), boardGrid = Color(0x2841DDF2), boardBorder = Color(0xFF248BA6),
        wallBorder = Color(0xFFF97D93), openBorder = Color(0xFF71D0FF),
        joystickGlow = Color(0x1741DDF2), joystickGlowClear = Color(0x0041DDF2), joystickRing = Color(0x3341DDF2),
        joystickInner = Color(0x1E41DDF2), joystickKnob = Color(0xFF41DDF2), joystickHighlight = Color(0xFFD9FAFF),
        joystickHint = Color(0xFF65AABB), overlay = Color(0xD9061627), homeTop = Color(0xFF0F3650), homeBottom = Color(0xFF04101F),
        scoreLabel = Color(0xFF9FCBDA), statsTile = Color(0xFF154562)
    )
    GameTheme.SUNSET -> ZaPalette(
        bg = Color(0xFF211222), panel = Color(0xFF352039), panel2 = Color(0xFF492A43),
        accent = Color(0xFFFF9872), muted = Color(0xFFD1ABC4), gold = Color(0xFFFFD36E), blue = Color(0xFFB7A3FF),
        primaryText = Color(0xFFFFF3EE), secondaryText = Color(0xFFF2D5DF), border = Color(0xFF704459), divider = Color(0xFF58334B),
        selectedPanel = Color(0xFF583048), chipBackground = Color(0xFF462A3E), buttonText = Color(0xFF361622),
        boardBackground = Color(0xFF28162B), boardGrid = Color(0x29FF9872), boardBorder = Color(0xFFA85F73),
        wallBorder = Color(0xFFFC5F79), openBorder = Color(0xFFBCA4FE),
        joystickGlow = Color(0x19FF9872), joystickGlowClear = Color(0x00FF9872), joystickRing = Color(0x35FF9872),
        joystickInner = Color(0x22FF9872), joystickKnob = Color(0xFFFF9872), joystickHighlight = Color(0xFFFFE4BC),
        joystickHint = Color(0xFFAA6E86), overlay = Color(0xD9211222), homeTop = Color(0xFF4A2540), homeBottom = Color(0xFF1A1128),
        scoreLabel = Color(0xFFD6ACC1), statsTile = Color(0xFF533148)
    )
    GameTheme.ROYAL -> ZaPalette(
        bg = Color(0xFF140C27), panel = Color(0xFF281742), panel2 = Color(0xFF362257),
        accent = Color(0xFFC6A0FF), muted = Color(0xFFB5A1D4), gold = Color(0xFFFFD567), blue = Color(0xFFA8B8FF),
        primaryText = Color(0xFFF8F0FF), secondaryText = Color(0xFFE2D4F7), border = Color(0xFF57417E), divider = Color(0xFF46346A),
        selectedPanel = Color(0xFF3B2860), chipBackground = Color(0xFF312149), buttonText = Color(0xFF211339),
        boardBackground = Color(0xFF1D1136), boardGrid = Color(0x28C6A0FF), boardBorder = Color(0xFF805CB1),
        wallBorder = Color(0xFFFF7198), openBorder = Color(0xFF8BB0FF),
        joystickGlow = Color(0x1BC6A0FF), joystickGlowClear = Color(0x00C6A0FF), joystickRing = Color(0x35C6A0FF),
        joystickInner = Color(0x23C6A0FF), joystickKnob = Color(0xFFC6A0FF), joystickHighlight = Color(0xFFF4DBFF),
        joystickHint = Color(0xFF9173B4), overlay = Color(0xDA140C27), homeTop = Color(0xFF352056), homeBottom = Color(0xFF120923),
        scoreLabel = Color(0xFFBDA9DA), statsTile = Color(0xFF3B2860)
    )
    GameTheme.DESERT -> ZaPalette(
        bg = Color(0xFFFFF1D8), panel = Color(0xFFFFF9ED), panel2 = Color(0xFFF5E2BC),
        accent = Color(0xFF9D431F), muted = Color(0xFF765943), gold = Color(0xFF94620B), blue = Color(0xFF3C7390),
        primaryText = Color(0xFF38251C), secondaryText = Color(0xFF674B38), border = Color(0xFFD6BA95), divider = Color(0xFFE0CAA8),
        selectedPanel = Color(0xFFF5D9AC), chipBackground = Color(0xFFF7E7CC), buttonText = Color(0xFFFFFFFF),
        boardBackground = Color(0xFFFFF7E9), boardGrid = Color(0x339D431F), boardBorder = Color(0xFFB98E65),
        wallBorder = Color(0xFFB84846), openBorder = Color(0xFF3C7996),
        joystickGlow = Color(0x229D431F), joystickGlowClear = Color(0x009D431F), joystickRing = Color(0x359D431F),
        joystickInner = Color(0x209D431F), joystickKnob = Color(0xFF9D431F), joystickHighlight = Color(0xFFFFDCA3),
        joystickHint = Color(0xFF977451), overlay = Color(0xC5FFF1D8), homeTop = Color(0xFFF1D6AB), homeBottom = Color(0xFFFFF6E5),
        scoreLabel = Color(0xFF816147), statsTile = Color(0xFFF7E0BB)
    )
    GameTheme.ROSE -> ZaPalette(
        bg = Color(0xFF220F1F), panel = Color(0xFF36172E), panel2 = Color(0xFF4A213C),
        accent = Color(0xFFFF83B9), muted = Color(0xFFD6A6BB), gold = Color(0xFFFFCC87), blue = Color(0xFFA5B7FF),
        primaryText = Color(0xFFFFF2F8), secondaryText = Color(0xFFF3CEDF), border = Color(0xFF704059), divider = Color(0xFF573049),
        selectedPanel = Color(0xFF572641), chipBackground = Color(0xFF46243A), buttonText = Color(0xFF330F28),
        boardBackground = Color(0xFF280F26), boardGrid = Color(0x29FF83B9), boardBorder = Color(0xFFAE5680),
        wallBorder = Color(0xFFFF597F), openBorder = Color(0xFF9EAAFF),
        joystickGlow = Color(0x1BFF83B9), joystickGlowClear = Color(0x00FF83B9), joystickRing = Color(0x36FF83B9),
        joystickInner = Color(0x23FF83B9), joystickKnob = Color(0xFFFF83B9), joystickHighlight = Color(0xFFFFD4EB),
        joystickHint = Color(0xFFB06A91), overlay = Color(0xD9220F1F), homeTop = Color(0xFF4A1D3A), homeBottom = Color(0xFF1B0B1B),
        scoreLabel = Color(0xFFDAB0C3), statsTile = Color(0xFF572641)
    )
}

// The game-board uses the actual purchased theme independently of PLAYFORM menus.
internal val LocalBoardPalette = staticCompositionLocalOf { paletteFor(GameTheme.NIGHT) }
internal fun playformPalette(): ZaPalette = paletteFor(GameTheme.LIGHT).copy(
    bg = Playform.Cream, panel = Playform.White, panel2 = Playform.White,
    accent = Playform.Coral, muted = Playform.TextSecondary,
    gold = Playform.GoldText, blue = Playform.Ink,
    primaryText = Playform.Ink, secondaryText = Playform.TextSecondary,
    border = Playform.Border, divider = Playform.Border,
    selectedPanel = Playform.PaleLavender, chipBackground = Playform.White,
    buttonText = Playform.Ink, joystickGlow = Playform.PaleLavender,
    joystickGlowClear = Color.Transparent, joystickRing = Playform.Lavender,
    joystickInner = Playform.PaleLavender, joystickKnob = Playform.Coral,
    joystickHighlight = Playform.White, joystickHint = Playform.TextSecondary,
    overlay = Color(0xCE11152A), homeTop = Playform.Cream,
    homeBottom = Playform.Cream, scoreLabel = Playform.TextSecondary,
    statsTile = Playform.White
)
internal val LocalZaPalette = staticCompositionLocalOf { playformPalette() }
// Android's animator-duration scale (including "Remove animations") is respected.
internal val LocalVisualMotionEnabled = staticCompositionLocalOf { true }

internal val Bg: Color @Composable get() = LocalZaPalette.current.bg
internal val Panel: Color @Composable get() = LocalZaPalette.current.panel
internal val Panel2: Color @Composable get() = LocalZaPalette.current.panel2
internal val Neon: Color @Composable get() = LocalZaPalette.current.accent
internal val Muted: Color @Composable get() = LocalZaPalette.current.muted
internal val Gold: Color @Composable get() = LocalZaPalette.current.gold
internal val Blue: Color @Composable get() = LocalZaPalette.current.blue
internal val PrimaryText: Color @Composable get() = LocalZaPalette.current.primaryText
internal val SecondaryText: Color @Composable get() = LocalZaPalette.current.secondaryText
internal val BorderColor: Color @Composable get() = LocalZaPalette.current.border
internal val DividerColor: Color @Composable get() = LocalZaPalette.current.divider
internal val SelectedPanel: Color @Composable get() = LocalZaPalette.current.selectedPanel
internal val ChipBackground: Color @Composable get() = LocalZaPalette.current.chipBackground
internal val ButtonText: Color @Composable get() = LocalZaPalette.current.buttonText
