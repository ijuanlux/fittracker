package com.juan.fittracker.ui.nearby

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.juan.fittracker.data.CookieMood
import com.juan.fittracker.data.NearbyGym
import com.juan.fittracker.data.NearbyGymsRepository
import com.juan.fittracker.data.RolaPhrases
import com.juan.fittracker.data.Sex
import com.juan.fittracker.data.UserProfile
import com.juan.fittracker.data.formatDistance
import com.juan.fittracker.ui.CookieAccessory
import com.juan.fittracker.ui.CookieAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Accent: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.primary
private val OnDark: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
private val BgDark: Color
    @androidx.compose.runtime.Composable get() = androidx.compose.material3.MaterialTheme.colorScheme.background
private val GoldStar = Color(0xFFFFD54F)

private sealed class NearbyState {
    data object Idle : NearbyState()
    data object NeedsPermission : NearbyState()
    data object Locating : NearbyState()
    data object NoLocation : NearbyState()
    data object Loading : NearbyState()
    data class Loaded(val gyms: List<NearbyGym>) : NearbyState()
    data class Error(val message: String) : NearbyState()
}


private fun gymScore(g: NearbyGym): Float {
    val distance = 1f / (1f + (g.distanceMeters / 1000f).toFloat())
    val website = if (g.website != null) 0.20f else 0f
    val phone = if (g.phone != null) 0.15f else 0f
    val address = if (g.address != null) 0.10f else 0f
    return distance + website + phone + address
}

private fun detectiveQuote(sex: Sex, key: Int): String =
    RolaPhrases.pick(RolaPhrases.detective, key, sex)

private fun recommendedQuote(sex: Sex, key: Int): String =
    RolaPhrases.pick(RolaPhrases.detectiveRecommended, key, sex)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(profile: UserProfile) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<NearbyState>(NearbyState.Idle) }
    var radiusKm by remember { mutableStateOf(5) }
    var quoteKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(45_000)
            quoteKey++
        }
    }
    val quote = remember(quoteKey, profile.sex) { detectiveQuote(profile.sex, quoteKey) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scope.launch { state = locateAndFetch(context, radiusKm) }
        } else {
            state = NearbyState.NeedsPermission
        }
    }

    fun refresh() {
        if (!hasLocationPermission(context)) {
            permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            return
        }
        scope.launch { state = locateAndFetch(context, radiusKm) }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            state = NearbyState.NeedsPermission
        } else {
            state = locateAndFetch(context, radiusKm)
        }
    }
    LaunchedEffect(radiusKm) {
        if (state is NearbyState.Loaded) {
            state = locateAndFetch(context, radiusKm)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Gym cerca", color = Accent, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(
                    "Gimnasios mapeados en OpenStreetMap",
                    color = OnDark.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                )
            }
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refrescar", tint = Accent)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CookieAvatar(
                modifier = Modifier
                    .size(110.dp)
                    .clickable { quoteKey++ },
                mood = CookieMood.Happy,
                accessory = CookieAccessory.MagnifyingGlass,
                isSpeaking = state is NearbyState.Loaded,
            )
            Spacer(Modifier.width(12.dp))
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.12f)),
            ) {
                Text(
                    text = quote,
                    modifier = Modifier.padding(14.dp),
                    color = OnDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1, 3, 5, 10).forEach { km ->
                FilterChip(
                    selected = radiusKm == km,
                    onClick = { radiusKm = km },
                    label = { Text("$km km") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                        labelColor = OnDark,
                        selectedContainerColor = Accent.copy(alpha = 0.25f),
                        selectedLabelColor = Accent,
                    ),
                )
            }
        }

        when (val s = state) {
            NearbyState.Idle, NearbyState.Locating, NearbyState.Loading -> CenteredLoader()
            NearbyState.NeedsPermission -> NeedsPermissionView(
                onGrant = { permLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
            )
            NearbyState.NoLocation -> NoLocationView(onRetry = { refresh() })
            is NearbyState.Error -> ErrorView(s.message, onRetry = { refresh() })
            is NearbyState.Loaded -> {
                if (s.gyms.isEmpty()) {
                    EmptyResultsView(radiusKm)
                } else {
                    val recommended = s.gyms.maxByOrNull(::gymScore)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (recommended != null) {
                            item {
                                RecommendedCard(
                                    gym = recommended,
                                    quote = recommendedQuote(profile.sex, quoteKey),
                                    onOpenMaps = { openInMaps(context, recommended) },
                                )
                            }
                            item {
                                Text(
                                    "Todos los resultados",
                                    color = OnDark.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                        items(s.gyms, key = { it.id }) { gym ->
                            GymCard(gym = gym, onOpenMaps = { openInMaps(context, gym) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedCard(gym: NearbyGym, quote: String, onOpenMaps: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Accent.copy(alpha = 0.15f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = GoldStar)
                Spacer(Modifier.width(6.dp))
                Text("Recomendado", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text(formatDistance(gym.distanceMeters), color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(gym.name, color = OnDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (gym.address != null) {
                Spacer(Modifier.height(4.dp))
                Text(gym.address, color = OnDark.copy(alpha = 0.7f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "“$quote”",
                color = OnDark.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenMaps,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = BgDark),
            ) {
                Text("Cómo llegar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GymCard(gym: NearbyGym, onOpenMaps: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = gym.name,
                    color = OnDark,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDistance(gym.distanceMeters),
                    color = Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (gym.address != null) {
                Spacer(Modifier.height(4.dp))
                Text(gym.address, color = OnDark.copy(alpha = 0.7f), fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenMaps,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = BgDark),
            ) {
                Text("Cómo llegar", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CenteredLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Accent)
    }
}

@Composable
private fun NeedsPermissionView(onGrant: () -> Unit) {
    InfoState(
        title = "Necesitamos tu ubicación",
        body = "Para encontrar gimnasios cerca de ti necesitamos permiso de ubicación aproximada.",
        actionLabel = "Permitir ubicación",
        onAction = onGrant,
    )
}

@Composable
private fun NoLocationView(onRetry: () -> Unit) {
    InfoState(
        title = "No pude obtener tu ubicación",
        body = "Activa el GPS o la red, sal a un sitio con señal y vuelve a intentar.",
        actionLabel = "Reintentar",
        onAction = onRetry,
        icon = Icons.Filled.LocationOff,
    )
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    InfoState(
        title = "Algo salió mal",
        body = message,
        actionLabel = "Reintentar",
        onAction = onRetry,
    )
}

@Composable
private fun EmptyResultsView(radiusKm: Int) {
    InfoState(
        title = "Sin gimnasios mapeados",
        body = "No encontré gimnasios en $radiusKm km en OpenStreetMap. Prueba con un radio mayor o contribuye en OSM.",
        actionLabel = null,
        onAction = {},
        icon = Icons.Filled.Search,
    )
}

@Composable
private fun InfoState(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Place,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = Accent.copy(alpha = 0.6f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = OnDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = OnDark.copy(alpha = 0.65f), fontSize = 14.sp)
        if (actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(actionLabel, color = Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private suspend fun locateAndFetch(context: Context, radiusKm: Int): NearbyState {
    val location = getCurrentLocation(context) ?: return NearbyState.NoLocation
    return try {
        val gyms = NearbyGymsRepository.fetchNearby(location.latitude, location.longitude, radiusKm * 1000)
        NearbyState.Loaded(gyms)
    } catch (e: Exception) {
        NearbyState.Error(e.message ?: "Error consultando OpenStreetMap")
    }
}

private suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
    val lm = context.getSystemService<LocationManager>() ?: return@withContext null
    try {
        lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    } catch (_: SecurityException) {
        null
    }
}

private fun openInMaps(context: Context, gym: NearbyGym) {
    val uri = Uri.parse("geo:${gym.lat},${gym.lng}?q=${gym.lat},${gym.lng}(${Uri.encode(gym.name)})")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
