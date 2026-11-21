# Smart Solar ProGuard / R8 keep rules
-keepattributes SourceFile,LineNumberTable
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

# Moshi & Retrofit DTOs
-keep class com.smartsolar.app.data.remote.** { *; }
-keep class com.smartsolar.app.data.local.StoredCartItemDto { *; }
-keep class com.smartsolar.app.data.local.StoredCookieDto { *; }
-keep class com.smartsolar.app.domain.model.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
