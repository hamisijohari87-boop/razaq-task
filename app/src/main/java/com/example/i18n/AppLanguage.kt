package com.example.i18n

enum class AppLanguage(
    val code: String,
    val nativeName: String,
    val isRtl: Boolean = false
) {
    KISWAHILI("sw", "Kiswahili", false),
    ENGLISH("en", "English", false),
    FRENCH("fr", "Français", false),
    ARABIC("ar", "العربية", true),
    SPANISH("es", "Español", false),
    PORTUGUESE("pt", "Português", false),
    GERMAN("de", "Deutsch", false),
    CHINESE("zh", "中文", false),
    HINDI("hi", "हिन्दी", false),
    TURKISH("tr", "Türkçe", false);

    companion object {
        val DEFAULT: AppLanguage = KISWAHILI

        fun fromCode(code: String?): AppLanguage {
            val clean = code?.trim()?.lowercase()
            return entries.firstOrNull { it.code == clean } ?: DEFAULT
        }
    }
}
