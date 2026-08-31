-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class org.biglau.data.** {
    *** Companion;
}
-keepclasseswithmembers class org.biglau.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
