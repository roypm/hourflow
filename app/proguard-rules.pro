# Modelos kotlinx.serialization (JSON backup)
-keepclassmembers class com.horastrabajo.app.data.export.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.horastrabajo.app.data.export.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room entities
-keep class com.horastrabajo.app.data.local.entity.** { *; }

# DataStore / Preferences
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# Composer (Compose compiler)
-keep class androidx.compose.** { *; }
