package com.ahmedzayed.zasnake.age

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import androidx.compose.ui.platform.LocalConfiguration
import com.ahmedzayed.zasnake.language.DeviceLanguage
import kotlinx.coroutines.launch
import kotlin.math.abs

private val WheelAccent = Color(0xFF00FF88)
private val WheelText = Color(0xFFE2EEE7)
private val WheelMuted = Color(0xFF97AAA2)
private val WheelRowHeight = 48.dp
private const val VisibleWheelRows = 5

private data class WheelChoice(val value: Int?, val title: String)

/** Neutral, mandatory first-run age screen: no suggested birth date and no skip control. */
@Composable
fun AgeGateScreen(onSubmit: (day: Int, month: Int, year: Int) -> Boolean) {
    val arabic = DeviceLanguage.fromLocale(LocalConfiguration.current.locales[0]) == "ar"
    var day by rememberSaveable { mutableStateOf<Int?>(null) }
    var month by rememberSaveable { mutableStateOf<Int?>(null) }
    var year by rememberSaveable { mutableStateOf<Int?>(null) }
    var error by rememberSaveable { mutableStateOf(false) }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val dayItems = remember { (1..31).map { WheelChoice(it, it.toString().padStart(2, '0')) } }
    val monthItems = remember(arabic) {
        val names = if (arabic) {
            listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        } else {
            listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        }
        names.mapIndexed { index, name -> WheelChoice(index + 1, name) }
    }
    val yearItems = remember(currentYear) {
        (currentYear downTo maxOf(1900, currentYear - 120)).map { WheelChoice(it, it.toString()) }
    }
    val dateComplete = day != null && month != null && year != null
    val valid = dateComplete && AgePolicy.classify(day!!, month!!, year!!) != null

    BackHandler(enabled = true) { /* No age-gate bypass with the system back gesture. */ }
    Box(
        Modifier.fillMaxSize().background(Color(0xFF050A0E)).safeDrawingPadding().padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("Za Snake", color = WheelAccent, fontSize = 34.sp, fontWeight = FontWeight.Black)
            Text(
                if (arabic) "ما تاريخ ميلادك؟" else "What is your date of birth?",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 23.sp,
                textAlign = TextAlign.Center
            )
            Text(
                if (arabic) "نستخدم فئتك العمرية لاختيار إعدادات مناسبة. لا يتم حفظ تاريخ ميلادك كاملًا."
                else "We use your age group to apply appropriate settings. Your full birth date is not stored.",
                color = Color(0xFFC5D5CE), fontSize = 13.sp, textAlign = TextAlign.Center
            )
            // All three wheels start on a blank placeholder. Nothing is selected for the player.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AgeDateWheel(
                    label = if (arabic) "اليوم" else "Day",
                    choices = dayItems,
                    selected = day,
                    onSelect = { day = it; error = false },
                    modifier = Modifier.weight(1f)
                )
                AgeDateWheel(
                    label = if (arabic) "الشهر" else "Month",
                    choices = monthItems,
                    selected = month,
                    onSelect = { month = it; error = false },
                    modifier = Modifier.weight(1.25f)
                )
                AgeDateWheel(
                    label = if (arabic) "السنة" else "Year",
                    choices = yearItems,
                    selected = year,
                    onSelect = { year = it; error = false },
                    modifier = Modifier.weight(1.15f)
                )
            }
            if (dateComplete && !valid) Text(
                if (arabic) "اختر تاريخ ميلاد صحيحًا وغير مستقبلي." else "Choose a valid date that is not in the future.",
                color = Color(0xFFFFC7A3), fontSize = 12.sp, textAlign = TextAlign.Center
            )
            if (error) Text(
                if (arabic) "تعذّر حفظ الاختيار، حاول مرة أخرى." else "Could not save your selection. Please retry.",
                color = Color(0xFFFF8B99), fontSize = 12.sp
            )
            Button(
                onClick = {
                    if (valid) error = !onSubmit(day!!, month!!, year!!)
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (arabic) "متابعة" else "Continue")
            }
        }
    }
}

/**
 * A real five-row, snapping date wheel. The initial centered row is an empty choice,
 * not an assumed birthday. The central highlight never changes the player's age
 * until a wheel settles or they tap an item.
 */
@Composable
private fun AgeDateWheel(
    label: String,
    choices: List<WheelChoice>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val allChoices = remember(choices) { listOf(WheelChoice(null, "—")) + choices }
    val initialIndex = remember(allChoices) {
        allChoices.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val scope = rememberCoroutineScope()
    val latestOnSelect by rememberUpdatedState(onSelect)

    LaunchedEffect(listState, allChoices) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
            val centered = layout.visibleItemsInfo.minByOrNull { info ->
                abs(info.offset + info.size / 2 - viewportCenter)
            }
            listState.isScrollInProgress to centered?.index
        }.collect { (scrolling, centeredIndex) ->
            if (!scrolling && centeredIndex != null) {
                latestOnSelect(allChoices[centeredIndex].value)
            }
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WheelMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier.fillMaxWidth().height(WheelRowHeight * VisibleWheelRows),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(WheelRowHeight)
                    .background(Color(0xFF153025), RoundedCornerShape(12.dp))
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize().semantics { contentDescription = label },
                state = listState,
                contentPadding = PaddingValues(vertical = WheelRowHeight * 2),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                itemsIndexed(allChoices, key = { index, _ -> index }) { index, option ->
                    Box(
                        modifier = Modifier.fillMaxWidth().height(WheelRowHeight)
                            .clickable {
                                scope.launch { listState.animateScrollToItem(index) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.title,
                            color = if (selected == option.value) WheelAccent else WheelText,
                            fontSize = if (selected == option.value) 19.sp else 16.sp,
                            fontWeight = if (selected == option.value) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
