# AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepattributes *Annotation*

# Kotlinx Serialization (if used later)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# pdfbox-android references this optional JPEG2000 decoder when available.
-dontwarn com.gemalto.jp2.JP2Decoder
