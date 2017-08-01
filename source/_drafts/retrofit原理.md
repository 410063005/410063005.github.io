---
title: retrofit原理
tags:
 - anroid
---

Retrofit是什么

> Type-safe HTTP client for Android and Java by Square, Inc.

Android和Java平台的类型安全的HTTP客户端。[来源][github]

---
# 介绍
在Retrofit中使用Java接口声明HTTP API：

```java
public interface GitHubService {
  @GET("users/{user}/repos")
  Call<List<Repo>> listRepos(@Path("user") String user);
}
```

`Retrofit`类负责生成`GitHubService`接口的具体实现。该具体实现返回的`Call`对象可用于向远程web服务器发起同步或异步的HTTP请求。

Retrofit中使用注解来描述HTTP请求：

+ 支持URL参数替换以及查询参数
+ 将对象转换成请求体(比如JSON或Protocol buffers)
+ Multipart request body以及文件上传

# API声明
接口方法以及参数的注解来说明如何发起HTTP请求。

## 请求方法
每个方法必须有一个HTTP注解，指定了请求方法以及相对URL。有5种内置的注解：`GET`, `POST`, `PUT`, `DELETE`, `HEAD`

## URL操作
请求的URL可以使用替换块或参数动态更新。

```java
@GET("group/{id}/users")
Call<List<User>> groupList(@Path("id") int groupId);
```

这里`{id}`是替换块，对应的参数是groupId，它由`@Path`注解，注解参数必须跟替换块中的名字保持一致。

## 请求体
由`@Body`注解的对象可以作为HTTP请求体。对象由`Retrofit`对象中指定的converter转换。如果没有指定converter，只能使用`RequestBody`。j

# 源码分析
```java
// Retrofit.java
  public <T> T create(final Class<T> service) {
    Utils.validateServiceInterface(service);
    if (validateEagerly) {
      eagerlyValidateMethods(service);
    }
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, @Nullable Object[] args)
              throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
              return method.invoke(this, args);
            }
            if (platform.isDefaultMethod(method)) {
              return platform.invokeDefaultMethod(method, service, proxy, args);
            }
            ServiceMethod<Object, Object> serviceMethod =
                (ServiceMethod<Object, Object>) loadServiceMethod(method);
            OkHttpCall<Object> okHttpCall = new OkHttpCall<>(serviceMethod, args);
            return serviceMethod.callAdapter.adapt(okHttpCall);
          }
        });
  }
```

`Retrofit.create()`方法为接口中的每个方法生成`ServiceMethod`

# 总结
思路很简单，但反射编程比较麻烦。抛开参数检查、异常处理、调试信息处理、bug规避等非关键代码，核心其实很容易理解

反射编程、解耦(CallAdapter, Converter由第三方实现)、IO操作导致参数检查、异常处理代码特别多。

TcpServiceMethod - 比 ServiceMethod 应该要简单很多 (感觉没必要实现，直接使用ServiceMethod)
TcpCall - 类似OkHttpCall 如何调用TcpClient

# 我的工作

问题: 协议导致不是单纯的body 解决办法: CmdMessage 原来从CommReq -> body 现在是 CmdMessage -> body
问题：响应中还包括status, message   解决办法: 原来从body -> Message 现在是 body -> StatusMesage

Retrofit原理浅析 http://www.jianshu.com/p/cd69c75d053e 

这篇文章非常不错。讲到了关键点。



----

https://blog.robinchutaux.com/blog/a-smart-way-to-use-retrofit/

https://news.realm.io/cn/news/droidcon-jake-wharton-simple-http-retrofit-2/

你真的会用Retrofit2吗?Retrofit2完全教程

http://www.jianshu.com/p/308f3c54abdd

Retrofit 2.0：有史以来最大的改进

http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0915/3460.html

你不知道的Retrofit缓存库RxCache

http://www.jcodecraeer.com/a/anzhuokaifa/2017/0112/7005.html

Retrofit 源码解读之离线缓存策略的实现

http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2016/0115/3873.html


使用Mockito、Robolectric和RxJava及Retrofit进行单元测试

http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0915/3458.html

----

[github]: https://github.com/square/retrofit

http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0915/3460.html
http://www.jianshu.com/p/308f3c54abdd
https://news.realm.io/cn/news/droidcon-jake-wharton-simple-http-retrofit-2/