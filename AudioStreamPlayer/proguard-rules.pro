# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp3
-dontwarn okhttp3.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase**
 # OkHttp platform used only on JVM and when Conscrypt dependency is available.
 -dontwarn okhttp3.internal.platform.ConscryptPlatform
 -dontwarn org.conscrypt.ConscryptHostnameVerifier
 #End

# Okio
-dontwarn okio.**

# Retrofit 2.X
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
# Moshkelat ziadi baraye mobham kardan 'SerializedName' dashtam ke ba inha hal shod
#Refrence https://community.guardsquare.com/t/what-if-i-use-gson-serialization/82
-keep class sun.misc.Unsafe** { *; }
-keep class com.google.gson.examples.android.model** { *; }
 -dontnote com.google.gson.**
 # GSON TypeAdapters are only referenced in annotations so ProGuard doesn't find their method usage
 -keepclassmembers,allowobfuscation,includedescriptorclasses class * extends com.google.gson.TypeAdapter {
     public <methods>;
 }
 # GSON TypeAdapterFactory is an interface, we need to keep the entire class, not just its members
 -keep,allowobfuscation,includedescriptorclasses class * implements com.google.gson.TypeAdapterFactory
 # GSON JsonDeserializer and JsonSerializer are interfaces, we need to keep the entire class, not just its members
 -keep,allowobfuscation,includedescriptorclasses class * implements com.google.gson.JsonDeserializer
 -keep,allowobfuscation,includedescriptorclasses class * implements com.google.gson.JsonSerializer
 # Ensure that all fields annotated with SerializedName will be kept
 -keepclassmembers,allowobfuscation class * {
     @com.google.gson.annotations.SerializedName <fields>;
 }
 ########################## Gson End ############################################

