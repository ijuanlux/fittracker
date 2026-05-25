package com.juan.fittracker.data

object FoodComments {

    private val comments: List<Pair<List<String>, List<String>>> = listOf(
        listOf("ajiaco") to listOf(
            "¡Qué nota ese ajiaco! Como el de La Macarena, marica.",
            "Hijuepucha, ajiaco con guascas frescas no tiene comparación.",
            "Ajiaco santafereño, eso sí es comida de rolo.",
        ),
        listOf("bandeja paisa", "bandeja", "paisa") to listOf(
            "Bandeja paisa, somos rolos pero igual le metemos, marica.",
            "¡Tremenda bandeja! Mañana toca sudarla en serio.",
            "Eso es plato pa' rendir todo el día.",
        ),
        listOf("tamal") to listOf(
            "¡Tamal tolimense! Con chocolatico santafereño es una vuelta.",
            "Tamal de domingo, la tradición no se rompe, __BRO__.",
            "Hijuepucha, tamal pesadito pero rico.",
        ),
        listOf("chuletón", "chuleton", "chuleta") to listOf(
            "¡Chuletón! Eso es comida brava, dele duro al gym mañana.",
            "Tremendo cortazo, __BRO__. Proteína al máximo.",
        ),
        listOf("caldo de costilla", "costilla") to listOf(
            "Caldo de costilla, el levantamuertos del rolo.",
            "Después de una trasnochada, esto te resucita.",
        ),
        listOf("changua") to listOf(
            "Changua. Como dicen los abuelos, levanta hasta a un muerto.",
            "Desayuno santafereño de raíz, __BRO__.",
        ),
        listOf("arepa") to listOf(
            "Arepa con quesito, clásico que nunca falla.",
            "Una arepita boyacense y ya, __BRO__.",
        ),
        listOf("tinto") to listOf(
            "Tinto bien cargado, eso sí.",
            "El combustible del rolo.",
        ),
        listOf("buñuelo") to listOf(
            "Buñuelos, ¡qué nota! Solo uno, ¿no? Bueno, dos.",
            "Buñuelito con natilla, vale la pena.",
        ),
        listOf("almojabana", "almojábana") to listOf(
            "Almojábana fresquita con tintico. Pura nota.",
            "De panadería de barrio, las mejores.",
        ),
        listOf("aguapanela") to listOf(
            "Aguapanela con quesito. Soporta cualquier crisis.",
            "Energía pura, marica.",
        ),
        listOf("empanada") to listOf(
            "Empanada con ají. Combo perfecto.",
        ),
        listOf("sancocho") to listOf(
            "¡Sancocho! Plato bandera, __BRO__.",
            "Domingo en familia, eso huele a sancocho.",
        ),
        listOf("pizza") to listOf(
            "Pizza, ¡qué pereza el lunes pero rico hoy!",
            "Ese cheat day va con cara y todo.",
        ),
        listOf("hamburguesa", "burger") to listOf(
            "Hamburguesa, dale. Mañana ensalada.",
        ),
        listOf("ensalada") to listOf(
            "¡Ensalada! Qué nivel, __BRO__.",
            "Verdecito, ahí vamos bien.",
        ),
    )

    fun commentFor(foodName: String, bro: String): String? {
        val needle = foodName.lowercase().trim()
        for ((keys, msgs) in comments) {
            if (keys.any { needle.contains(it) }) {
                return msgs.random().replace("__BRO__", bro)
            }
        }
        return null
    }
}
