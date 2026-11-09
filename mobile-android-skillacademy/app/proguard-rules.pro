# app/proguard-rules.pro — Blindaje ProGuard / R8 bajo ISO 27001 A.8.8

-repackageclasses "com.skillacademy.secure"
-allowaccessmodification
-optimizationpasses 5

# Eliminación automática de todas las llamadas a android.util.Log en compilación Release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Ofuscar nombres de clases, campos y métodos para impedir lectura con jadx
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-keepattributes *Annotation*,Signature,InnerClasses

# Mantener modelos de Compose y entidades
-keep class com.skillacademy.mobile.data.model.** { *; }
-keep class com.skillacademy.mobile.data.remote.** { *; }

