package com.juan.fittracker.data

enum class CookieMood {
    Energetic, Happy, Neutral, Tired, Sleepy, Lazy, Stuffed
}

data class GalletoideInsight(
    val mood: CookieMood,
    val quote: String,
    val advice: String,
)

object Insights {
    fun compute(
        sleep: SleepStats?,
        steps: Long,
        activeKcal: Double,
        hr: HeartRateStats?,
        hasAnyData: Boolean,
        sex: Sex = Sex.Unspecified,
        intakeKcal: Int = 0,
        targetKcal: Int = 0,
        refreshKey: Int = 0,
    ): GalletoideInsight {
        // Stuffed has priority
        if (targetKcal > 0 && intakeKcal > targetKcal * 1.3f) {
            return GalletoideInsight(
                mood = CookieMood.Stuffed,
                quote = RolaPhrases.pick(RolaPhrases.stuffed, refreshKey, sex),
                advice = RolaPhrases.pick(RolaPhrases.stuffedAdvice, refreshKey, sex),
            )
        }

        if (!hasAnyData || sleep == null) {
            return GalletoideInsight(
                mood = CookieMood.Neutral,
                quote = RolaPhrases.pick(RolaPhrases.noData, refreshKey, sex),
                advice = RolaPhrases.pick(RolaPhrases.noDataAdvice, refreshKey, sex),
            )
        }

        val hours = sleep.hours
        val sleptWell = hours >= 7f && hours <= 9.5f
        val sleptBad = hours in 0.1f..5.9f && sleep.hasData
        val tooMuch = hours > 9.5f
        val veryActive = steps >= 12000
        val active = steps in 7000..11999L
        val sedentary = steps < 3000

        return when {
            sleptBad && sedentary -> GalletoideInsight(
                CookieMood.Sleepy,
                RolaPhrases.pick(RolaPhrases.sleepySedentary, refreshKey, sex),
                RolaPhrases.pick(RolaPhrases.sleepySedentaryAdvice, refreshKey, sex),
            )
            sleptBad && veryActive -> GalletoideInsight(
                CookieMood.Tired,
                RolaPhrases.pick(RolaPhrases.tiredAfterActivity, refreshKey, sex),
                RolaPhrases.pick(RolaPhrases.tiredAdvice, refreshKey, sex),
            )
            sleptBad -> GalletoideInsight(
                CookieMood.Sleepy,
                RolaPhrases.pick(RolaPhrases.sleepyBadSleep, refreshKey, sex),
                RolaPhrases.pick(RolaPhrases.sleepyBadSleepAdvice, refreshKey, sex),
            )
            sleptWell && veryActive -> GalletoideInsight(
                CookieMood.Energetic,
                RolaPhrases.pick(RolaPhrases.energetic, refreshKey, sex),
                RolaPhrases.pick(RolaPhrases.energeticAdvice, refreshKey, sex),
            )
            sleptWell && active -> GalletoideInsight(
                CookieMood.Happy,
                RolaPhrases.pick(RolaPhrases.happy, refreshKey, sex),
                RolaPhrases.pick(RolaPhrases.happyAdvice, refreshKey, sex),
            )
            sleptWell && sedentary -> GalletoideInsight(
                CookieMood.Lazy,
                RolaPhrases.pick(RolaPhrases.lazySleptWell, refreshKey, sex),
                RolaPhrases.pick(RolaPhrases.lazySleptWellAdvice, refreshKey, sex),
            )
            tooMuch -> GalletoideInsight(
                CookieMood.Lazy,
                RolaPhrases.pick(
                    RolaPhrases.tooMuchSleep, refreshKey, sex,
                    "hours" to hours.toInt().toString(),
                ),
                RolaPhrases.pick(RolaPhrases.tooMuchSleepAdvice, refreshKey, sex),
            )
            veryActive -> GalletoideInsight(
                CookieMood.Tired,
                RolaPhrases.pick(
                    RolaPhrases.veryActive, refreshKey, sex,
                    "steps" to steps.toString(),
                ),
                RolaPhrases.pick(RolaPhrases.veryActiveAdvice, refreshKey, sex),
            )
            sedentary -> GalletoideInsight(
                CookieMood.Lazy,
                RolaPhrases.pick(RolaPhrases.sedentary, refreshKey, sex),
                RolaPhrases.pick(RolaPhrases.sedentaryAdvice, refreshKey, sex),
            )
            else -> GalletoideInsight(
                CookieMood.Neutral,
                RolaPhrases.pick(RolaPhrases.neutral, refreshKey, sex),
                RolaPhrases.pick(RolaPhrases.neutralAdvice, refreshKey, sex),
            )
        }
    }
}
