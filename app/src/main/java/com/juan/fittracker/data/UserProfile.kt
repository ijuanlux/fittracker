package com.juan.fittracker.data

data class UserProfile(
    val age: Int,
    val sex: Sex,
    val frequency: TrainingFrequency,
    val heightCm: Int,
    val weightKg: Int,
) {
    companion object {
        val Default = UserProfile(
            age = 25,
            sex = Sex.Unspecified,
            frequency = TrainingFrequency.Regular,
            heightCm = 170,
            weightKg = 70,
        )
    }
}

enum class Sex(val label: String) {
    Male("Hombre"),
    Female("Mujer"),
    Unspecified("Prefiero no decir"),
}

enum class TrainingFrequency(val label: String, val days: String) {
    Casual("Casual", "1-2 días/semana"),
    Regular("Regular", "3-4 días/semana"),
    Intenso("Intenso", "5-6 días/semana"),
    Atleta("Atleta", "Todos los días"),
}
