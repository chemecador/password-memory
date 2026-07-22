package com.chemecador.passwordmemory.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette: indigo primary (the launcher icon gradient), cyan secondary and a rose tertiary.
 * Tones follow the Material 3 roles so the schemes in [PasswordMemoryTheme] stay contrast-safe.
 */

// Indigo
val Indigo10 = Color(0xFF0A0060)
val Indigo20 = Color(0xFF1B138F)
val Indigo30 = Color(0xFF3634BF)
val Indigo40 = Color(0xFF4E4CD8)
val Indigo80 = Color(0xFFC0C1FF)
val Indigo90 = Color(0xFFE2E0FF)

// Cyan
val Cyan10 = Color(0xFF001F26)
val Cyan20 = Color(0xFF00363F)
val Cyan30 = Color(0xFF004E5A)
val Cyan40 = Color(0xFF00687A)
val Cyan80 = Color(0xFF57D7EF)
val Cyan90 = Color(0xFFB4ECF7)

// Rose
val Rose10 = Color(0xFF3E001D)
val Rose20 = Color(0xFF5E1133)
val Rose30 = Color(0xFF7B2949)
val Rose40 = Color(0xFF9A3F61)
val Rose80 = Color(0xFFFFB1C6)
val Rose90 = Color(0xFFFFD9E2)

// Neutrals
val Neutral0 = Color(0xFF000000)
val Neutral6 = Color(0xFF0E0E13)
val Neutral10 = Color(0xFF131318)
val Neutral12 = Color(0xFF1B1B21)
val Neutral17 = Color(0xFF1F1F25)
val Neutral22 = Color(0xFF2A2930)
val Neutral24 = Color(0xFF35343B)
val Neutral30 = Color(0xFF47464F)
val Neutral60 = Color(0xFF787680)
val Neutral70 = Color(0xFF918F9A)
val Neutral80 = Color(0xFFC8C5D0)
val Neutral90 = Color(0xFFE4E1EC)
val Neutral92 = Color(0xFFE5E1EB)
val Neutral94 = Color(0xFFEAE7F1)
val Neutral96 = Color(0xFFF0EDF6)
val Neutral98 = Color(0xFFF6F3FB)
val Neutral99 = Color(0xFFFCFAFF)
val Neutral100 = Color(0xFFFFFFFF)

val NeutralInverse20 = Color(0xFF303036)
val NeutralInverse90 = Color(0xFFE5E1E9)

/**
 * Fixed gradient pairs for service avatars. They are picked deterministically from the service
 * name, so the list gets some colour without the entries ever shuffling. White text is legible on
 * every pair in both themes, which is why these are not scheme roles.
 */
val AvatarGradients: List<Pair<Color, Color>> = listOf(
    Color(0xFF7C6CFF) to Color(0xFF4338CA),
    Color(0xFF00C2E0) to Color(0xFF00708A),
    Color(0xFFF857A6) to Color(0xFFB4145E),
    Color(0xFF34D399) to Color(0xFF0E8C63),
    Color(0xFFFBBF24) to Color(0xFFC77800),
    Color(0xFF9B7BFF) to Color(0xFF6027C7)
)

// Error (Material 3 baseline)
val Error10 = Color(0xFF410002)
val Error20 = Color(0xFF690005)
val Error30 = Color(0xFF93000A)
val Error40 = Color(0xFFBA1A1A)
val Error80 = Color(0xFFFFB4AB)
val Error90 = Color(0xFFFFDAD6)
