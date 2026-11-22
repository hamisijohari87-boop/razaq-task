# RAZAQ TASK Release R8 / ProGuard Rules

# Preserve line numbers for Crash/StackTrace diagnostics without exposing source paths
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Preserve @Keep classes & Firestore POJOs (UserEntity, SocialTaskEntity, etc.)
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
    @com.google.firebase.firestore.DocumentId *;
    @com.google.firebase.firestore.ServerTimestamp *;
}
-keepnames class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** { *; }

# Keep Jetpack Credential Manager & Google Identity Services classes
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**

# Keep Kotlin Coroutines & Flow internals used by Firestore tasks-ktx
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
