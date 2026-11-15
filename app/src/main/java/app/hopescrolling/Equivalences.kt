package app.hopescrolling

import java.text.NumberFormat

/** Ce qu'on peut faire avec le temps récupéré. Les durées sont des moyennes. */
enum class Equivalence(
    val minutesPerUnit: Double,
    val singular: String,
    val plural: String,
    val assumption: String,
) {
    BOOKS(360.0, "livre lu", "livres lus", "un livre ≈ 6 h de lecture"),
    PAGES(2.0, "page lue", "pages lues", "une page ≈ 2 min"),
    WALK_KM(12.0, "km de marche", "km de marche", "5 km/h"),
    MOVIES(120.0, "film", "films", "un film ≈ 2 h"),
    WORKOUTS(45.0, "séance de sport", "séances de sport", "une séance ≈ 45 min"),
    LESSONS(15.0, "leçon de langue", "leçons de langue", "une leçon ≈ 15 min"),
    NIGHTS(480.0, "nuit de sommeil", "nuits de sommeil", "une nuit ≈ 8 h");

    fun value(minutes: Int) = minutes / minutesPerUnit

    /** « 2,5 livres lus », « 1 film », « 0,4 séance de sport ». */
    fun format(minutes: Int): String {
        val v = value(minutes)
        val nf = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = if (v < 10) 1 else 0
        }
        val n = nf.format(v)
        return "$n ${if (v >= 2) plural else singular}"
    }

    companion object {
        /** Une équivalence parlante pour une petite durée (message de blocage). */
        fun short(minutes: Int): String = when {
            minutes < 30 -> PAGES.format(minutes)
            minutes < 90 -> WORKOUTS.format(minutes)
            minutes < 360 -> MOVIES.format(minutes)
            else -> BOOKS.format(minutes)
        }

        /** Les quatre équivalences du tableau de bord. */
        val DASHBOARD = listOf(BOOKS, WALK_KM, MOVIES, WORKOUTS)
    }
}

fun formatDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${"%02d".format(minutes % 60)}"
}
