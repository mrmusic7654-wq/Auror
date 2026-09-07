# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class aura.**$$serializer { *; }
-keepclassmembers class aura.** {
    *** Companion;
}
-keepclasseswithmembers class aura.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data models used via reflection/room where necessary
-keep class aura.orchestrator.data.** { *; }
-keep class aura.core.** { *; }
