package com.example.i18n

object RazakTranslations {
    fun forLanguage(language: AppLanguage): AppStrings = when (language) {
        AppLanguage.KISWAHILI -> TranslationsSw
        AppLanguage.ENGLISH -> TranslationsEn
        AppLanguage.FRENCH -> TranslationsFr
        AppLanguage.ARABIC -> TranslationsAr
        AppLanguage.SPANISH -> TranslationsEs
        AppLanguage.PORTUGUESE -> TranslationsPt
        AppLanguage.GERMAN -> TranslationsDe
        AppLanguage.CHINESE -> TranslationsZh
        AppLanguage.HINDI -> TranslationsHi
        AppLanguage.TURKISH -> TranslationsTr
    }
}
