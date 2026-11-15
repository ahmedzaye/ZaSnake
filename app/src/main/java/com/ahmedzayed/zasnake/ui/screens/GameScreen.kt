package com.ahmedzayed.zasnake.ui

import com.ahmedzayed.zasnake.ads.RewardedAds
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import androidx.compose.ui.unit.Dp
import com.ahmedzayed.zasnake.GameViewModel
import com.ahmedzayed.zasnake.game.AppScreen
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.ControlStyle
import com.ahmedzayed.zasnake.game.Direction
import com.ahmedzayed.zasnake.game.GameMode
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.game.SnakeHeadStyle
import com.ahmedzayed.zasnake.game.SnakeBodyType
import com.ahmedzayed.zasnake.game.SpecialType
import com.ahmedzayed.zasnake.game.EngineEvent
import com.ahmedzayed.zasnake.game.VisualCue
import kotlin.math.abs
import kotlin.math.min

@Composable
internal fun GameScreen(state: AppUiState, viewModel: GameViewModel, rewardedAds: RewardedAds?, showOverlay: Boolean) {
    val lang = state.settings.language
    val layoutDirection = if (lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val engine = viewModel.engine
    val dPadSelected = state.settings.controlStyle == ControlStyle.D_PAD
    BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            // Physical controls stay independent of RTL. Each style gets a full-height touch zone.
            // In landscape the D-Pad is centered in a dedicated side panel to preserve board size.
            val controlsOnLeft = !dPadSelected && state.settings.joystickLeftLandscape
            androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(Modifier.fillMaxSize().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (controlsOnLeft) {
                        GameControlArea(Modifier.weight(0.34f).fillMaxHeight(), lang, !showOverlay,
                            state.settings.controlStyle, viewModel::requestDirection, true, landscape = true)
                    }
                    Column(Modifier.weight(0.66f).fillMaxHeight()) {
                        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                            GameScoreBar(state, engine, Modifier.fillMaxWidth().height(60.dp), onPause = viewModel::requestPause)
                        }
                        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            val side = min(maxWidth.value, maxHeight.value).dp
                            GameBoard(engine, state.frameVersion, state.settings.skin, state.settings.headStyle, state.settings.bodyType, state.visualCues, Modifier.size(side))
                        }
                    }
                    if (!controlsOnLeft) {
                        GameControlArea(Modifier.weight(0.34f).fillMaxHeight(), lang, !showOverlay,
                            state.settings.controlStyle, viewModel::requestDirection, false, landscape = true)
                    }
                }
            }
        } else {
            // Reserve the same minimum control height as before, then size the board explicitly.
            // Any unused vertical space belongs to the touch region, not a blank board container.
            val horizontalPadding = 8.dp
            val verticalPadding = 6.dp
            val scoreHeight = 60.dp
            val minimumControlHeight = if (dPadSelected) 178.dp else 132.dp
            val boardSide = min(
                (maxWidth - horizontalPadding * 2).value,
                (maxHeight - verticalPadding * 2 - scoreHeight - minimumControlHeight).value
            ).coerceAtLeast(0f).dp
            Column(Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = verticalPadding)) {
                GameScoreBar(state, engine, Modifier.fillMaxWidth().height(scoreHeight), onPause = viewModel::requestPause)
                Box(Modifier.fillMaxWidth().height(boardSide), contentAlignment = Alignment.Center) {
                    GameBoard(engine, state.frameVersion, state.settings.skin, state.settings.headStyle, state.settings.bodyType, state.visualCues, Modifier.size(boardSide))
                }
                GameControlArea(
                    Modifier.fillMaxWidth().weight(1f), lang, !showOverlay,
                    state.settings.controlStyle, viewModel::requestDirection, false, landscape = false
                )
            }
        }
        val graceSeconds = viewModel.resumeGraceSeconds.value
        if (state.screen == AppScreen.GAME && graceSeconds > 0) {
            Box(Modifier.fillMaxSize().padding(top = 62.dp), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.clip(RoundedCornerShape(14.dp))
                    .background(Panel.copy(alpha = .93f))
                    .border(1.dp, Neon.copy(alpha = .6f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$graceSeconds", color = Playform.Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(if (lang == "ar") "حرّك الثعبان للبدء" else "Steer to continue",
                        color = SecondaryText, fontSize = 10.sp)
                }
            }
        }
        if (engine.shieldRemainingMs > 0L && state.screen == AppScreen.GAME) {
            Box(Modifier.fillMaxSize().padding(top = 54.dp), contentAlignment = Alignment.TopCenter) {
                Text("🛡 ${(engine.shieldRemainingMs + 999L) / 1000L}s", color = Playform.Ink,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }
        if (showOverlay) {
            Box(Modifier.fillMaxSize().background(LocalZaPalette.current.overlay), contentAlignment = Alignment.Center) {
                if (state.screen == AppScreen.PAUSE) PauseCard(state, viewModel)
                else GameOverCard(state, viewModel, rewardedAds)
            }
        }
    }
}

@Composable
internal fun GameControlArea(
    modifier: Modifier,
    language: String,
    enabled: Boolean,
    style: ControlStyle,
    onDirection: (Direction) -> Unit,
    leftSidePreference: Boolean,
    landscape: Boolean
) {
    if (style == ControlStyle.D_PAD) {
        // Physical direction buttons must not be reversed by Arabic RTL layout.
        // Only the D-Pad is forced LTR; all other Arabic screens remain RTL.
        androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            DPadArea(modifier, enabled, onDirection, landscape)
        }
    } else {
        JoystickArea(modifier, language, enabled, onDirection, leftSidePreference)
    }
}

@Composable
internal fun DPadArea(modifier: Modifier, enabled: Boolean, onDirection: (Direction) -> Unit, landscape: Boolean) {
    val palette = LocalZaPalette.current
    val buttonSize = if (landscape) 44.dp else 52.dp
    val gap = if (landscape) 2.dp else 4.dp
    Box(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(Brush.radialGradient(listOf(palette.joystickGlow, palette.joystickGlowClear))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(gap)) {
            DPadButton(Direction.UP, buttonSize, enabled, onDirection)
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                DPadButton(Direction.LEFT, buttonSize, enabled, onDirection)
                Box(Modifier.size(buttonSize).clip(RoundedCornerShape(10.dp)).background(palette.joystickInner))
                DPadButton(Direction.RIGHT, buttonSize, enabled, onDirection)
            }
            DPadButton(Direction.DOWN, buttonSize, enabled, onDirection)
        }
    }
}

@Composable
internal fun DPadButton(direction: Direction, size: Dp, enabled: Boolean, onDirection: (Direction) -> Unit) {
    var pressed by remember(direction) { mutableStateOf(false) }
    val palette = LocalZaPalette.current
    val icon = when (direction) {
        Direction.UP -> Icons.Rounded.KeyboardArrowUp
        Direction.DOWN -> Icons.Rounded.KeyboardArrowDown
        Direction.LEFT -> Icons.Rounded.KeyboardArrowLeft
        Direction.RIGHT -> Icons.Rounded.KeyboardArrowRight
    }
    Box(
        Modifier.size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed && enabled) palette.joystickKnob else palette.joystickInner)
            .border(1.dp, if (pressed && enabled) palette.joystickHighlight else palette.joystickRing, RoundedCornerShape(12.dp))
            .pointerInput(enabled, direction) {
                if (enabled) detectTapGestures(onPress = {
                    pressed = true
                    try {
                        onDirection(direction) // Direction change on DOWN, not after lifting a finger.
                        tryAwaitRelease()
                    } finally {
                        pressed = false
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = direction.name,
            tint = if (pressed && enabled) Playform.Ink else palette.joystickKnob,
            modifier = Modifier.size(size * 0.68f))
    }
}

@Composable
internal fun GameScoreBar(state: AppUiState, engine: com.ahmedzayed.zasnake.game.SnakeEngine, modifier: Modifier, onPause: () -> Unit) {
    val lang = state.settings.language
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ScoreItem(tx(lang, "score"), engine.score.toString(), Modifier.weight(1f), highlightOnChange = true)
        ScoreItem(tx(lang, "best_score"), state.stats.bestScore.toString(), Modifier.weight(1f))
        ScoreItem(if (engine.mode == GameMode.TIME_ATTACK) tx(lang, "time") else tx(lang, "level"),
            if (engine.mode == GameMode.TIME_ATTACK) ((engine.timeRemainingMs + 999) / 1000).toString() else engine.level.toString(), Modifier.weight(1f))
        IconButtonFrame(onClick = onPause, modifier = Modifier.size(42.dp)) {
            androidx.compose.material3.Icon(Icons.Rounded.Pause, tx(lang, "pause_title"), tint = Playform.Ink)
        }
    }
}

@Composable
internal fun ScoreItem(label: String, value: String, modifier: Modifier, highlightOnChange: Boolean = false) {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(value, highlightOnChange) {
        if (highlightOnChange && value != "0") {
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(durationMillis = 280))
        }
    }
    Column(
        modifier.fillMaxHeight().clip(RoundedCornerShape(14.dp)).background(Playform.White)
            .border(1.dp, Playform.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = LocalZaPalette.current.scoreLabel, fontSize = 11.sp,
            lineHeight = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text(value, color = Playform.Ink, fontSize = 18.sp, lineHeight = 21.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = 1f + .10f * pulse.value
                scaleY = 1f + .10f * pulse.value
            },
            fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun GameBoard(engine: com.ahmedzayed.zasnake.game.SnakeEngine, frameVersion: Long, skin: SnakeSkin, headStyle: SnakeHeadStyle, bodyType: SnakeBodyType, cues: List<VisualCue>, modifier: Modifier) {
    val palette = LocalBoardPalette.current
    val motionEnabled = LocalVisualMotionEnabled.current
    Canvas(modifier = modifier.clip(RoundedCornerShape(12.dp)).border(2.dp, palette.boardBorder, RoundedCornerShape(12.dp))) {
        // Capturing the frame marker invalidates the draw closure while the ViewModel ticks.
        if (frameVersion == Long.MIN_VALUE) return@Canvas
        drawRect(palette.boardBackground)
        val cell = size.width / com.ahmedzayed.zasnake.game.SnakeEngine.GRID_SIZE
        for (i in 0..com.ahmedzayed.zasnake.game.SnakeEngine.GRID_SIZE) {
            val p = i * cell
            drawLine(palette.boardGrid, Offset(p, 0f), Offset(p, size.height), 0.5f)
            drawLine(palette.boardGrid, Offset(0f, p), Offset(size.width, p), 0.5f)
        }
        val borderColor = if (engine.isWallsEffective) palette.wallBorder else palette.openBorder
        drawRect(borderColor, style = Stroke(width = 2f))

        // Regular red apple.
        val foodX = (engine.food.x + 0.5f) * cell
        val foodY = (engine.food.y + 0.5f) * cell
        drawCircle(Color(0xAAFF3366), cell * 0.42f, Offset(foodX, foodY))
        drawCircle(Color(0xFFFF3366), cell * 0.30f, Offset(foodX, foodY + cell * 0.03f))
        drawLine(palette.accent, Offset(foodX, foodY - cell * 0.23f), Offset(foodX + cell * 0.11f, foodY - cell * 0.39f), cell * 0.07f, cap = StrokeCap.Round)

        engine.specials.forEach { special ->
            val cx = (special.cell.x + 0.5f) * cell
            val cy = (special.cell.y + 0.5f) * cell
            if (special.type == SpecialType.GOLDEN) {
                drawCircle(palette.gold.copy(alpha = 0.30f), cell * 0.48f, Offset(cx, cy))
                drawCircle(palette.gold, cell * 0.31f, Offset(cx, cy))
                drawCircle(Color(0xFFFFF1A8), cell * 0.10f, Offset(cx - cell * .08f, cy - cell * .09f))
            } else {
                drawLine(Color(0xFFBDE8FF), Offset(cx - cell * .22f, cy - cell * .2f), Offset(cx + cell * .2f, cy + cell * .2f), cell * .10f, cap = StrokeCap.Round)
                drawLine(Color(0xFFFFC5D3), Offset(cx + cell * .22f, cy - cell * .2f), Offset(cx - cell * .2f, cy + cell * .2f), cell * .10f, cap = StrokeCap.Round)
            }
        }

        val colors = snakeSkinColors(skin)
        val segments = engine.interpolatedSnake()
        if (bodyType != SnakeBodyType.SEGMENTED) {
            // Small dark silhouette under each style to keep light skins visible on light themes.
            val centers = segments.map { (gx, gy) -> Offset((gx + .5f) * cell, (gy + .5f) * cell) }
            val outline = Color(0xFF0D1820).copy(alpha = .80f)
            drawSnakeBodyByType(bodyType, centers, cell * 1.075f, listOf(outline))
            drawSnakeBodyByType(bodyType, centers, cell, colors)
            centers.firstOrNull()?.let { center ->
                drawStyledSnakeHead(center, cell * 1.075f, outline, headStyle, engine.direction, silhouetteOnly = true)
                drawStyledSnakeHead(center, cell, colors.first(), headStyle, engine.direction)
            }
        } else {
            for (i in segments.indices.reversed()) {
                val (gx, gy) = segments[i]
                val inset = cell * 0.10f
                val color = if (i == 0 || colors.size == 2) colors[if (i == 0) 0 else 1] else colors[(i % (colors.size - 1)) + 1]
                val left = gx * cell + inset
                val top = gy * cell + inset
                if (i != 0) {
                    val borderInset = cell * .065f
                    drawRoundRect(Color(0xFF0D1820).copy(alpha = .85f),
                        Offset(gx * cell + borderInset, gy * cell + borderInset),
                        Size(cell - borderInset * 2, cell - borderInset * 2), CornerRadius(cell * .27f))
                    drawRoundRect(color.copy(alpha = 0.88f), Offset(left, top),
                        Size(cell - inset * 2, cell - inset * 2), CornerRadius(cell * 0.25f))
                } else {
                    val center = Offset(gx * cell + cell / 2f, gy * cell + cell / 2f)
                    drawStyledSnakeHead(center, cell * 1.075f, Color(0xFF0D1820), headStyle,
                        engine.direction, silhouetteOnly = true)
                    drawStyledSnakeHead(center, cell, color, headStyle, engine.direction)
                }
            }
        }

        // Event-driven neon/arcade highlights. Bounded to four transient cues and
        // rendered on the existing game frame; no additional animation timer or physics.
        if (motionEnabled) {
            val now = SystemClock.elapsedRealtime()
            cues.forEach { cue ->
                val duration = when (cue.kind) {
                    EngineEvent.Type.GOLDEN -> 480f
                    EngineEvent.Type.SCISSORS -> 350f
                    else -> 270f
                }
                val elapsed = (now - cue.startedAtElapsedMs).toFloat()
                if (elapsed >= 0f && elapsed < duration) {
                    val progress = (elapsed / duration).coerceIn(0f, 1f)
                    val fade = (1f - progress) * (1f - progress)
                    val center = Offset((cue.cell.x + 0.5f) * cell, (cue.cell.y + 0.5f) * cell)
                    when (cue.kind) {
                        EngineEvent.Type.FOOD -> {
                            drawCircle(Color(0xFFFF6D90).copy(alpha = fade * .8f),
                                radius = cell * (.22f + progress * .85f), center = center,
                                style = Stroke(width = cell * .10f))
                        }
                        EngineEvent.Type.GOLDEN -> {
                            drawCircle(palette.gold.copy(alpha = fade * .85f),
                                radius = cell * (.38f + progress * 1.5f), center = center,
                                style = Stroke(width = cell * .13f))
                            for (k in 0 until 6) {
                                val angle = k * (kotlin.math.PI.toFloat() / 3f)
                                val inner = cell * (.50f + progress * .9f)
                                val outer = inner + cell * .20f
                                val unit = Offset(kotlin.math.cos(angle), kotlin.math.sin(angle))
                                drawLine(palette.gold.copy(alpha = fade),
                                    center + unit * inner, center + unit * outer,
                                    strokeWidth = cell * .08f, cap = StrokeCap.Round)
                            }
                        }
                        EngineEvent.Type.SCISSORS -> {
                            val span = cell * (.25f + progress * .75f)
                            val alphaColor = palette.blue.copy(alpha = fade)
                            drawLine(alphaColor, center + Offset(-span, -span), center + Offset(span, span),
                                strokeWidth = cell * .09f, cap = StrokeCap.Round)
                            drawLine(alphaColor, center + Offset(-span, span), center + Offset(span, -span),
                                strokeWidth = cell * .09f, cap = StrokeCap.Round)
                        }
                        EngineEvent.Type.GAME_OVER -> Unit
                    }
                }
            }
        }
    }
}

@Composable
internal fun JoystickArea(
    modifier: Modifier,
    language: String,
    enabled: Boolean,
    onDirection: (Direction) -> Unit,
    leftSidePreference: Boolean
) {
    var active by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(Offset.Zero) }
    var knob by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val palette = LocalZaPalette.current
    val radiusPx = with(density) { 56.dp.toPx() }
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.radialGradient(listOf(palette.joystickGlow, palette.joystickGlowClear))).pointerInput(enabled, leftSidePreference) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            // The ring must remain completely within the available control region.
            val marginX = min(radiusPx, size.width / 2f)
            val marginY = min(radiusPx, size.height / 2f)
            anchor = Offset(
                down.position.x.coerceIn(marginX, size.width - marginX),
                down.position.y.coerceIn(marginY, size.height - marginY)
            )
            knob = anchor
            active = true
            val pointerId = down.id
            var keepTracking = true
            while (keepTracking) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                val p = change.position
                knob = Offset(p.x.coerceIn(0f, size.width.toFloat()), p.y.coerceIn(0f, size.height.toFloat()))
                val dx = p.x - down.position.x
                val dy = p.y - down.position.y
                if (maxOf(abs(dx), abs(dy)) > with(density) { 17.dp.toPx() }) {
                    if (abs(dx) > abs(dy)) onDirection(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                    else onDirection(if (dy > 0) Direction.DOWN else Direction.UP)
                }
                change.consume()
                keepTracking = change.pressed
            }
            active = false
        }
    }, contentAlignment = Alignment.Center) {
        if (active) {
            // Overlay only: the floating joystick never affects parent measurement.
            Canvas(Modifier.matchParentSize()) {
                val r = min(radiusPx, min(size.width, size.height) * 0.30f)
                drawCircle(palette.joystickRing, r, anchor, style = Stroke(width = 3f))
                drawCircle(palette.joystickInner, r, anchor)
                val offset = knob - anchor
                val maxTravel = r * 0.56f
                val length = kotlin.math.sqrt(offset.x * offset.x + offset.y * offset.y)
                val limited = if (length > maxTravel && length > 0f) offset * (maxTravel / length) else offset
                drawCircle(palette.joystickKnob, r * 0.38f, anchor + limited)
                drawCircle(palette.joystickHighlight, r * 0.20f, anchor + limited + Offset(-r * .07f, -r * .08f))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                androidx.compose.material3.Icon(Icons.Rounded.TouchApp, null, tint = palette.joystickHint, modifier = Modifier.size(25.dp))
                Text(tx(language, "tap_to_steer"), color = palette.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}
