package com.ahmedzayed.zasnake.game

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** A single UTC day on all devices. UTC avoids different local-midnight claim IDs after cloud sync. */
private fun utcDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
    isLenient = false
}

fun dailyUtcDate(epochMillis: Long = System.currentTimeMillis()): String = utcDateFormat().format(Date(epochMillis))

private fun previousUtcDay(day: String): String {
    val parser = utcDateFormat()
    val date = parser.parse(day) ?: return ""
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    calendar.time = date
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    return parser.format(calendar.time)
}

val DAILY_LOGIN_COIN_SEQUENCE = listOf(10, 15, 20, 25, 30, 40, 60)

data class DailyDeviceProgress(
    val apples: Long = 0,
    val completedGames: Long = 0,
    val bestRoundScore: Int = 0
) {
    fun merge(other: DailyDeviceProgress) = DailyDeviceProgress(
        apples = maxOf(apples, other.apples),
        completedGames = maxOf(completedGames, other.completedGames),
        bestRoundScore = maxOf(bestRoundScore, other.bestRoundScore)
    )
}

data class DailyProgress(val apples: Long = 0, val completedGames: Long = 0, val bestRoundScore: Int = 0)

enum class DailyChallenge(val id: String, val icon: String, val arName: String, val enName: String,
                          val arDescription: String, val enDescription: String, val target: Long, val coins: Int) {
    APPLES("apples20", "🍎", "جامع التفاح", "Apple Collector", "اجمع 20 تفاحة اليوم", "Eat 20 apples today", 20, 15),
    GAMES("games3", "🎮", "اللاعب النشيط", "Active Player", "أكمل 3 مباريات اليوم", "Finish 3 games today", 3, 15),
    SCORE("score30", "🏆", "صائد النقاط", "Score Hunter", "حقق 30 نقطة في مباراة اليوم", "Reach 30 points in one game today", 30, 15);

    fun progress(value: DailyProgress): Long = when (this) {
        APPLES -> value.apples
        GAMES -> value.completedGames
        SCORE -> value.bestRoundScore.toLong()
    }

    fun claimId(date: String): String = "$date:$id"
}

/** All daily coin income is ledger-backed; no rewards are granted just by opening the screen. */
fun PlayerStats.dailyProgress(date: String): DailyProgress {
    val byDevice = dailyActivity[date].orEmpty().values
    return DailyProgress(
        apples = byDevice.sumOf { it.apples },
        completedGames = byDevice.sumOf { it.completedGames },
        bestRoundScore = byDevice.maxOfOrNull { it.bestRoundScore } ?: 0
    )
}

fun PlayerStats.recordDailyActivity(date: String, deviceId: String, change: (DailyDeviceProgress) -> DailyDeviceProgress): PlayerStats {
    if (date.isBlank() || deviceId.isBlank() || date < latestDailyDate) return this
    val perDevice = dailyActivity[date].orEmpty()
    val updated = change(perDevice[deviceId] ?: DailyDeviceProgress())
    // Keep recent progress only; permanent claim ledgers are never trimmed.
    val activity = (dailyActivity + (date to (perDevice + (deviceId to updated))))
        .toSortedMap().entries.toList().takeLast(35).associate { it.key to it.value }
    return copy(dailyActivity = activity, latestDailyDate = maxOf(latestDailyDate, date))
}

fun PlayerStats.touchDailyDate(date: String): PlayerStats =
    if (date >= latestDailyDate) copy(latestDailyDate = date) else this

fun PlayerStats.claimDailyChallenge(date: String, challenge: DailyChallenge): PlayerStats {
    if (date.isBlank() || date < latestDailyDate) return this
    val key = challenge.claimId(date)
    if (key in claimedDailyChallenges || challenge.progress(dailyProgress(date)) < challenge.target) return this
    return copy(claimedDailyChallenges = claimedDailyChallenges + (key to challenge.coins),
                latestDailyDate = maxOf(date, latestDailyDate))
}

/** Consecutive *claimed* days (not merely opened days) determine the 7-day loop. */
fun PlayerStats.nextDailyLoginDay(date: String): Int {
    val previous = claimedDailyLogins[previousUtcDay(date)] ?: return 1
    return if (previous in 1..7) (previous % 7) + 1 else 1
}

fun PlayerStats.claimDailyLogin(date: String): PlayerStats {
    if (date.isBlank() || date < latestDailyDate || date in claimedDailyLogins) return this
    // An older date cannot be claimed after a later claim (e.g. changing the system clock back).
    if (claimedDailyLogins.keys.any { it > date }) return this
    val dayIndex = nextDailyLoginDay(date)
    return copy(claimedDailyLogins = claimedDailyLogins + (date to dayIndex),
                latestDailyDate = maxOf(date, latestDailyDate))
}
