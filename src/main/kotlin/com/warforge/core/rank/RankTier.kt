package com.warforge.core.rank

enum class RankTier(
    val displayName: String,
    val color: String,
    val minPoints: Int,
    val maxPoints: Int,
    val icon: String
) {
    BRONZE  ("Bronze",       "&6",  0,    999,   "⚙"),
    SILVER  ("Silver",       "&7",  1000, 2499,  "✦"),
    GOLD    ("Gold",         "&e",  2500, 4999,  "★"),
    PLATINUM("Platinum",     "&b",  5000, 9999,  "◆"),
    DIAMOND ("Diamond",      "&9",  10000,19999, "❖"),
    MASTER  ("Master",       "&d",  20000,34999, "✪"),
    GRANDMASTER("GrandMaster","&5", 35000,49999, "⚜"),
    LEGEND  ("Legend",       "&6&l",50000,Int.MAX_VALUE,"♛");

    companion object {
        fun fromPoints(points: Int): RankTier =
            values().lastOrNull { points >= it.minPoints } ?: BRONZE
    }

    fun formatted(): String = "$color${icon} $displayName"
}
