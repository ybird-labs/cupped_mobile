package cafe.cupped.app.locale

interface DateTimeFormatter {
    fun formatShortDate(epochMillis: Long): String
    fun formatRelativeTime(epochMillis: Long, nowEpochMillis: Long): String
}

interface NumberFormatter {
    fun formatDecimal(value: Double): String
    fun formatPercent(value: Double): String
    fun formatRating(value: Double, scale: Int): String
}
