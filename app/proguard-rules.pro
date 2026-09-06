-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class dev.kicker.hometiles.data.** {
    *** Companion;
}
-keepclasseswithmembers class dev.kicker.hometiles.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
