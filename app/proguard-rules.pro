# Preserve JSON-reflected and PDF/OCR integration metadata while allowing application code optimization.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-dontwarn org.bouncycastle.**
-dontwarn org.spongycastle.**
-dontwarn javax.activation.**
-dontwarn javax.xml.bind.**

# ML Kit discovers registrars through manifest metadata.
-keep class com.google.mlkit.** { *; }
-keep class com.google.firebase.components.ComponentRegistrar { *; }

# Broadcast receivers and Android components are referenced by the manifest.
-keep class com.jarvis.aegis.MainActivity { *; }
-keep class com.jarvis.aegis.accessibility.AegisAccessibilityService { *; }
-keep class com.jarvis.aegis.boot.** { *; }
-keep class com.jarvis.aegis.ui.lock.AegisLockActivity { *; }
