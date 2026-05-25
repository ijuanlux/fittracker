package com.juan.fittracker.data

object Levels {
    // Cumulative XP thresholds for each level. Index = level - 1.
    private val thresholds = intArrayOf(
        0,    // Lv 1
        15,   // Lv 2
        30,   // Lv 3
        50,   // Lv 4
        70,   // Lv 5
        95,   // Lv 6
        125,  // Lv 7
        160,  // Lv 8
        200,  // Lv 9
        250,  // Lv 10
    )

    const val MAX_LEVEL: Int = 10

    fun level(totalXp: Int): Int {
        var lvl = 1
        for (i in thresholds.indices) {
            if (totalXp >= thresholds[i]) lvl = i + 1 else break
        }
        return lvl
    }

    fun xpForLevel(lvl: Int): Int =
        thresholds.getOrNull(lvl.coerceIn(1, MAX_LEVEL) - 1) ?: 0

    fun xpForNextLevel(lvl: Int): Int? =
        if (lvl >= MAX_LEVEL) null else thresholds[lvl]

    fun totalXp(unlocks: List<AchievementUnlock>): Int =
        unlocks.mapNotNull { Achievement.byId(it.id)?.xp }.sum()
}
