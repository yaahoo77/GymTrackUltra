# Room, Hilt, Navigation i kotlinx.serialization dostarczają własne reguły R8.
# Poniżej zabezpieczenie dla tras nawigacji (@Serializable) — bez nich release może zgubić serializery.

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.gymtrack.ultra.**$$serializer { *; }
-keepclassmembers class com.gymtrack.ultra.** {
    *** Companion;
}
-keepclasseswithmembers class com.gymtrack.ultra.** {
    kotlinx.serialization.KSerializer serializer(...);
}
