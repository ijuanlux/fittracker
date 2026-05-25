package com.juan.fittracker.data

object MuscleClassifier {
    // Order matters: more specific keywords first.
    private val rules: List<Pair<String, List<String>>> = listOf(
        "Glúteo" to listOf(
            "hip thrust", "puente glúteo", "puente gluteo",
            "patada glúteo", "patada gluteo", "patada de mula",
            "abducción", "abduccion",
        ),
        "Pierna" to listOf(
            "sentadilla", "prensa de pierna", "prensa",
            "peso muerto rumano",
            "curl femoral", "curl de pierna",
            "extensión de cuádriceps", "extension de cuadriceps",
            "extensión cuádriceps", "extension cuadriceps",
            "gemelo", "zancada", "lunge", "búlgara", "bulgara",
            "step up", "goblet",
        ),
        "Espalda" to listOf(
            "dominada", "remo", "jalón", "jalon",
            "peso muerto", "hiperextensión", "hiperextension",
            "pull over", "encogimiento dorsal",
        ),
        "Pecho" to listOf(
            "press banca", "press inclinado", "press declinado",
            "aperturas", "aperturas con mancuernas",
            "fondos en paralelas", "peck deck", "cruces",
            "pec deck",
        ),
        "Hombros" to listOf(
            "press militar", "press arnold", "press de hombro", "press hombro",
            "elevación lateral", "elevacion lateral",
            "elevación frontal", "elevacion frontal",
            "pájaro", "pajaro",
            "face pull",
            "encogimientos",
        ),
        "Tríceps" to listOf(
            "press francés", "press frances",
            "extensión tríceps", "extension triceps",
            "extensión polea tríceps", "extension polea triceps",
            "fondos tríceps", "fondos triceps",
            "patada de tríceps", "patada de triceps",
            "kickback",
        ),
        "Bíceps" to listOf(
            "curl bíceps", "curl biceps",
            "curl con barra", "curl martillo",
            "predicador", "scott", "concentrado",
        ),
        "Cardio" to listOf(
            "cinta", "bici", "bicicleta", "cuerda",
            "burpee", "mountain climber", "caminata",
            "correr", "carrera", "running", "elíptica", "eliptica",
            "remo cardio", "rower",
        ),
        "Core" to listOf(
            "plancha", "abdomin", "crunch", "russian twist",
            "elevación pierna", "elevacion pierna",
            "rueda abdominal",
        ),
    )

    fun classify(exerciseName: String, routineNote: String?): String {
        val needle = exerciseName.lowercase()
        for ((group, keywords) in rules) {
            if (keywords.any { needle.contains(it) }) return group
        }
        // Fallback: derive from routine note (e.g., "Rutina Pierna")
        if (routineNote != null && routineNote.startsWith("Rutina ")) {
            val rn = routineNote.removePrefix("Rutina ").lowercase()
            return when {
                "pierna" in rn -> "Pierna"
                "pecho" in rn -> "Pecho"
                "espalda" in rn -> "Espalda"
                "hombro" in rn -> "Hombros"
                "cardio" in rn -> "Cardio"
                "full" in rn -> "Full body"
                else -> "Otros"
            }
        }
        return "Otros"
    }
}
