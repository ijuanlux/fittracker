package com.juan.fittracker.data

enum class NutritionStatus(val label: String, val description: String) {
    Deficit("Déficit", "Comiste menos de lo que gastaste"),
    Maintenance("Mantenimiento", "Estás en equilibrio"),
    Surplus("Superávit", "Comiste más de lo que gastaste"),
}

data class CaloriesBalance(
    val intake: Int,
    val burnedTotal: Int,
    val target: Int,
    val deltaToTarget: Int,
    val status: NutritionStatus,
)

object Nutrition {
    fun bmr(profile: UserProfile): Float {
        val w = profile.weightKg.toFloat()
        val h = profile.heightCm.toFloat()
        val age = profile.age
        val base = 10f * w + 6.25f * h - 5f * age
        return when (profile.sex) {
            Sex.Male -> base + 5f
            Sex.Female -> base - 161f
            Sex.Unspecified -> base - 78f
        }
    }

    fun activityFactor(frequency: TrainingFrequency): Float = when (frequency) {
        TrainingFrequency.Casual -> 1.375f
        TrainingFrequency.Regular -> 1.55f
        TrainingFrequency.Intenso -> 1.725f
        TrainingFrequency.Atleta -> 1.9f
    }

    fun targetKcal(profile: UserProfile): Float = bmr(profile) * activityFactor(profile.frequency)

    fun balance(
        profile: UserProfile,
        intakeKcal: Int,
        activeKcal: Double,
        gymKcal: Float,
    ): CaloriesBalance {
        val bmrVal = bmr(profile)
        val target = targetKcal(profile)
        val burnedTotal = (bmrVal + activeKcal.toFloat() + gymKcal).toInt()
        val delta = intakeKcal - target.toInt()
        val status = when {
            delta < -300 -> NutritionStatus.Deficit
            delta > 300 -> NutritionStatus.Surplus
            else -> NutritionStatus.Maintenance
        }
        return CaloriesBalance(
            intake = intakeKcal,
            burnedTotal = burnedTotal,
            target = target.toInt(),
            deltaToTarget = delta,
            status = status,
        )
    }
}

data class FoodPreset(val name: String, val kcal: Int, val emoji: String = "🍽")

enum class FoodCuisine(val label: String) {
    Rola("Rolas"),
    Mediterranea("Mediterráneas"),
}

val rolaQuickFoods = listOf(
    FoodPreset("Tamal tolimense", 600, "🫔"),
    FoodPreset("Ajiaco", 400, "🥣"),
    FoodPreset("Bandeja paisa", 1100, "🍛"),
    FoodPreset("Chuletón", 800, "🥩"),
    FoodPreset("Caldo de costilla", 250, "🍲"),
    FoodPreset("Changua", 220, "🍳"),
    FoodPreset("Arepa con queso", 280, "🫓"),
    FoodPreset("Almojábana", 200, "🥯"),
    FoodPreset("Buñuelo", 150, "🍩"),
    FoodPreset("Aguapanela", 60, "🍵"),
    FoodPreset("Tinto", 5, "☕"),
    FoodPreset("Empanada", 220, "🥟"),
    FoodPreset("Lechona", 700, "🐖"),
    FoodPreset("Sancocho de gallina", 500, "🍗"),
    FoodPreset("Frijoles con chicharrón", 550, "🫘"),
    FoodPreset("Patacón con hogao", 300, "🍌"),
    FoodPreset("Pandebono", 150, "🥯"),
    FoodPreset("Pandeyuca", 180, "🥖"),
    FoodPreset("Mazorca asada con queso", 260, "🌽"),
    FoodPreset("Mute santandereano", 450, "🍲"),
    FoodPreset("Sobrebarriga", 500, "🥩"),
    FoodPreset("Mondongo", 400, "🍲"),
    FoodPreset("Cuchuco de trigo", 280, "🌾"),
    FoodPreset("Obleas con arequipe", 260, "🍪"),
    FoodPreset("Postre de tres leches", 350, "🍰"),
    FoodPreset("Café con leche", 80, "☕"),
    FoodPreset("Avena (vaso)", 160, "🥛"),
)

val mediterraneanQuickFoods = listOf(
    FoodPreset("Ensalada César", 250, "🥗"),
    FoodPreset("Paella", 600, "🥘"),
    FoodPreset("Tortilla española", 300, "🍳"),
    FoodPreset("Gazpacho", 150, "🥣"),
    FoodPreset("Hummus con pan pita", 250, "🫓"),
    FoodPreset("Salmón a la plancha", 350, "🐟"),
    FoodPreset("Pollo a la plancha", 280, "🍗"),
    FoodPreset("Pasta carbonara", 600, "🍝"),
    FoodPreset("Pizza margarita", 700, "🍕"),
    FoodPreset("Bocadillo de jamón", 400, "🥖"),
    FoodPreset("Pan con tomate", 150, "🍞"),
    FoodPreset("Falafel", 330, "🧆"),
    FoodPreset("Yogur natural", 80, "🥛"),
    FoodPreset("Lentejas estofadas", 250, "🫘"),
    FoodPreset("Croquetas (4 ud)", 280, "🥟"),
    FoodPreset("Cocido madrileño", 700, "🍲"),
)

fun quickFoodsFor(cuisine: FoodCuisine): List<FoodPreset> = when (cuisine) {
    FoodCuisine.Rola -> rolaQuickFoods
    FoodCuisine.Mediterranea -> mediterraneanQuickFoods
}

fun emojiForFoodName(name: String): String? {
    val needle = name.trim()
    return rolaQuickFoods.firstOrNull { it.name.equals(needle, ignoreCase = true) }?.emoji
        ?: mediterraneanQuickFoods.firstOrNull { it.name.equals(needle, ignoreCase = true) }?.emoji
}
