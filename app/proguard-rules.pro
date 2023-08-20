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

-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keepattributes *Annotation*

# アプリ
-keep public class to.sava.cloudmarksandroid.** {*;}
-keep public class com.google.api.** {*;}

# OkHttp
-dontwarn org.apache.http.client.config.RequestConfig$Builder
-dontwarn org.apache.http.client.config.RequestConfig
-dontwarn org.apache.http.client.methods.HttpPatch
-dontwarn org.apache.http.config.Registry
-dontwarn org.apache.http.config.RegistryBuilder
-dontwarn org.apache.http.config.SocketConfig$Builder
-dontwarn org.apache.http.config.SocketConfig
-dontwarn org.apache.http.conn.DnsResolver
-dontwarn org.apache.http.conn.HttpClientConnectionManager
-dontwarn org.apache.http.conn.HttpConnectionFactory
-dontwarn org.apache.http.conn.SchemePortResolver
-dontwarn org.apache.http.conn.socket.LayeredConnectionSocketFactory
-dontwarn org.apache.http.conn.socket.PlainConnectionSocketFactory
-dontwarn org.apache.http.conn.ssl.SSLConnectionSocketFactory
-dontwarn org.apache.http.impl.client.CloseableHttpClient
-dontwarn org.apache.http.impl.client.HttpClientBuilder
-dontwarn org.apache.http.impl.conn.PoolingHttpClientConnectionManager
-dontwarn org.apache.http.impl.conn.SystemDefaultRoutePlanner
