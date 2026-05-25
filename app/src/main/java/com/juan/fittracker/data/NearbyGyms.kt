package com.juan.fittracker.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class NearbyGym(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val distanceMeters: Double,
    val address: String?,
    val phone: String?,
    val website: String?,
)

object NearbyGymsRepository {
    private val client by lazy { HttpClient(Android) }

    suspend fun fetchNearby(lat: Double, lng: Double, radiusMeters: Int = 5000): List<NearbyGym> =
        withContext(Dispatchers.IO) {
            val query = """
                [out:json][timeout:25];
                (
                  node["leisure"="fitness_centre"](around:$radiusMeters,$lat,$lng);
                  way["leisure"="fitness_centre"](around:$radiusMeters,$lat,$lng);
                  node["sport"="fitness"](around:$radiusMeters,$lat,$lng);
                  way["sport"="fitness"](around:$radiusMeters,$lat,$lng);
                );
                out body center;
            """.trimIndent()

            val body = client.post("https://overpass-api.de/api/interpreter") {
                setBody(query)
            }.bodyAsText()

            parseOverpass(body, lat, lng)
        }

    private fun parseOverpass(json: String, userLat: Double, userLng: Double): List<NearbyGym> {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
        val elements = root.optJSONArray("elements") ?: return emptyList()
        val seen = mutableSetOf<String>()
        val out = mutableListOf<NearbyGym>()
        for (i in 0 until elements.length()) {
            val el = elements.optJSONObject(i) ?: continue
            val tags = el.optJSONObject("tags") ?: continue
            val name = tags.optString("name").takeIf { it.isNotBlank() } ?: continue
            val (gymLat, gymLng) = when (el.optString("type")) {
                "node" -> el.optDouble("lat", Double.NaN) to el.optDouble("lon", Double.NaN)
                else -> {
                    val c = el.optJSONObject("center") ?: continue
                    c.optDouble("lat", Double.NaN) to c.optDouble("lon", Double.NaN)
                }
            }
            if (gymLat.isNaN() || gymLng.isNaN()) continue
            val id = "${el.optString("type")}-${el.optLong("id")}"
            if (!seen.add(id)) continue
            val street = tags.optString("addr:street").takeIf { it.isNotBlank() }
            val num = tags.optString("addr:housenumber").takeIf { it.isNotBlank() }
            val address = listOfNotNull(street, num).joinToString(" ").ifBlank { null }
            out += NearbyGym(
                id = id,
                name = name,
                lat = gymLat,
                lng = gymLng,
                distanceMeters = haversineMeters(userLat, userLng, gymLat, gymLng),
                address = address,
                phone = tags.optString("phone").takeIf { it.isNotBlank() }
                    ?: tags.optString("contact:phone").takeIf { it.isNotBlank() },
                website = tags.optString("website").takeIf { it.isNotBlank() }
                    ?: tags.optString("contact:website").takeIf { it.isNotBlank() },
            )
        }
        return out.sortedBy { it.distanceMeters }
    }
}

fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return 2 * r * asin(sqrt(a))
}

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m" else "%.1f km".format(meters / 1000.0)
