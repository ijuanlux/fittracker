package com.juan.fittracker.data

enum class FoodVibe { Empty, Fit, Neutral, Heavy }

object FoodClassifier {
    private val heavyKeywords = listOf(
        "pizza", "hamburguesa", "burger", "bandeja paisa", "chuletón", "chuleton",
        "lechona", "tres leches", "buñuelo", "buñuelos", "fritanga", "obleas",
        "pasta carbonara", "carbonara", "tamal", "salchipapa", "mondongo",
        "mute", "papas fritas", "patacones", "frijoles con chicharrón",
        "sobrebarriga", "empanada", "croquetas", "cocido madrileño",
    )

    private val fitKeywords = listOf(
        "ensalada", "pollo a la plancha", "salmón", "salmon", "hummus",
        "gazpacho", "claras de huevo", "claras", "avena", "fruta", "verduras",
        "atún", "atun", "yogur natural", "tortilla española", "ajiaco",
        "caldo de costilla", "changua", "pan con tomate", "lentejas",
        "pollo asado", "pescado", "brócoli", "espinacas",
    )

    fun classify(todayMeals: List<MealEntry>, targetKcal: Int): FoodVibe {
        if (todayMeals.isEmpty()) return FoodVibe.Empty
        val text = todayMeals.joinToString(" ") { it.name.lowercase() }
        val heavyHits = heavyKeywords.count { text.contains(it) }
        val fitHits = fitKeywords.count { text.contains(it) }
        val totalKcal = todayMeals.sumOf { it.kcal }
        val avgKcal = totalKcal / todayMeals.size

        val isOverTarget = targetKcal > 0 && totalKcal > targetKcal * 1.2f
        val isUnderTarget = targetKcal > 0 && totalKcal < targetKcal * 0.6f

        return when {
            heavyHits >= 2 || isOverTarget || avgKcal > 600 -> FoodVibe.Heavy
            fitHits >= 2 || (avgKcal < 350 && !isUnderTarget) -> FoodVibe.Fit
            else -> FoodVibe.Neutral
        }
    }

    fun poolFor(vibe: FoodVibe): List<String> = when (vibe) {
        FoodVibe.Empty -> RolaPhrases.foodEmptyAmbient
        FoodVibe.Fit -> RolaPhrases.foodFitPraise
        FoodVibe.Neutral -> RolaPhrases.foodNeutralReactive
        FoodVibe.Heavy -> RolaPhrases.foodHeavyTease
    }
}
