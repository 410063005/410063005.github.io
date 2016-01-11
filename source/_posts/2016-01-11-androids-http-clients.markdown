---
layout: post
title: "androids-http-clients"
date: 2016-01-11 17:38:06 +0800
comments: true
categories: android
published: true
---
本文介绍了Android平台上的HttpClient。
<!--more-->
大部分联网App使用HTTP收发数据。Android平台上包括两种HTTP client：HttpURLConnection和ApacheHTTPClient。都支持HTTPS，流式上传和下载，可配置的超时时间，IPv6和连接池。

# Apache HTTP Client
[DefaultHttpClient][]及其兄弟[AndroidHttpClient][]是可扩展的HTTP client，它们适用于Web浏览器。它们有庞大且灵活的API。它们的实现很稳定，bug较少。但是，过多的API导致我们很难在不破坏兼容性的情况下对其进行改进。Android项目组基于Apache HTTP Client活跃地开发。

# HttpURLConnection
[HttpURLConnection][]是一个通用的，轻量级和HTTP client，适用于多数应用。这个类一开始很简陋，但它的API易于持续地改进。

Froyo之前，HttpURLConnection有些让人沮丧的bug。实际上，在InputStream上调用`close()`会[破坏连接池][2939]。只好通过关闭连接池来规避这个问题：

```
private void disableConnectionReuseIfNecessary() {
    // HTTP connection reuse which was buggy pre-froyo
    if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
        System.setProperty("http.keepAlive", "false");
    }
}
```

在Gingerbread中，我们在HttpURLConnection中增加了响应压缩。HttpURLConnection会自动在发出的响应中增加这个请求头，并处理对应的响应：

```
Accept-Encoding: gzip
```

为那些可以支持压缩的客户端在Web Server中进行配置，可以利用这里的gzip压缩特性。如果响应压缩有问题，[文档][doc]中讲了如何关闭这个特性。

由于HTTP的`Content-Length`头返回压缩后的大小，对没解压的数据调用`getContentLength()`是不正确的。你应该读取整个响应流直到`InputStream.read()`返回-1。

在Gingerbread中还对HTTPS作了一些改进。HttpsURLConnection尝试跟那些允许多个HTTPS主机共享同一IP的Server Name Indication(SNI)进行连接。它还支持压缩和会话票据。如果连接失败，它会自动禁用这些特性后重试。HttpsURLConnection跟最新的后台服务器连接时更高效，同时能跟旧的后台服务器兼容。

在Icecream Sandwich中，添加了响应缓存。当加载缓存后，HTTP请求可能有以下三种情况：

+ 可以直接从本地缓存中拿到"Fully cached responses"。由于不必发起网络连接，这类响应几乎可以立即返回
+ 得到"Conditionally cached responses"， 需要去Web服务器上验证缓存是否有效。客户端发送类似于"Give me /foo.png if it changed since yesterday"的请求，而服务器要么返回最新的内容要么返回`304 Not Modified`状态码。如果内容没有发生改变，则不会进行下载。
+ 来自Web的是不可缓存的响应(Uncached responses)。These responses will get stored in the response cache for later (???我没明白)

可以使用反射在支持HTTP缓存的设备上打开这个特性。下面的示例代码打开IceCream Sandwich上的HTTP缓存特性，而不会影响其他版本的使用：

```
private void enableHttpResponseCache() {
    try {
        long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
        File httpCacheDir = new File(getCacheDir(), "http");
        Class.forName("android.net.http.HttpResponseCache")
            .getMethod("install", File.class, long.class)
            .invoke(null, httpCacheDir, httpCacheSize);
    } catch (Exception httpResponseCacheNotAvailable) {
    }
}
```

你应该配置Web服务器以在HTTP响应中设置缓存头。

# 哪个client更好
Eclair和Froyo版本中Apache HTTP client的bug更少。这些版本中最好用Apache HTTP client

而Gingerbread及以后版本中，最好使用HttpURLConnection。简单的API和更小的体积非常适合Android。透明压缩和响应缓存减少了网络开销，提高速度并减少耗电。新的应用应该使用HttpURLConnection，我们努力让它变得更好用。

[doc]: http://developer.android.com/reference/java/net/HttpURLConnection.html
[2939]: http://code.google.com/p/android/issues/detail?id=2939
[ref]: http://android-developers.blogspot.com/2011/09/androids-http-clients.html