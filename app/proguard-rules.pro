# SleepWhisper ProGuard / R8 rules.
# Targets: Hilt, Room, kotlinx.serialization, Media3, Compose.

# Keep stack traces useful in Play Console crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- Hilt / Dagger ----------
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.Module class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.InstallIn class *
# Generated Hilt components.
-keep class dagger.hilt.internal.** { *; }
-keep class **_HiltModules$* { *; }
-keep class **_HiltComponents$* { *; }

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ---------- kotlinx.serialization ----------
# Generated serializer companions are reflectively located via Kotlin metadata.
-keepclassmembers,allowshrinking,allowoptimization @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclassmembers class **$Companion {
    *** serializer(...);
}
-keep,includedescriptorclasses class com.lizhi1026.sleepwhisper.model.**$$serializer { *; }
-keepclassmembers class com.lizhi1026.sleepwhisper.model.** {
    *** Companion;
}
# Enums are referenced by name in JSON.
-keepclassmembers,allowoptimization enum com.lizhi1026.sleepwhisper.model.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- Media3 ----------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ---------- AndroidX lifecycle / DataStore ----------
-keep class androidx.lifecycle.** { *; }
-keep class androidx.datastore.** { *; }

# ---------- Compose ----------
# Compose runtime relies on reflection-free codegen; default rules in
# `proguard-android-optimize.txt` already cover its public surface. Nothing extra needed here.

# ---------- Project models (defensive) ----------
-keep class com.lizhi1026.sleepwhisper.model.** { *; }
