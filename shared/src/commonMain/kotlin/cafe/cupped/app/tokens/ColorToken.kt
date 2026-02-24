package cafe.cupped.app.tokens

data class ColorToken(val value: ULong) {
    val alpha: Float get() = ((value shr 24) and 0xFFu).toFloat() / 255f
    val red: Float get() = ((value shr 16) and 0xFFu).toFloat() / 255f
    val green: Float get() = ((value shr 8) and 0xFFu).toFloat() / 255f
    val blue: Float get() = (value and 0xFFu).toFloat() / 255f
}
