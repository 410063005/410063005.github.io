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

http://www.jianshu.com/p/cd69c75d053e

[github]: https://github.com/square/retrofit

http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0915/3460.html
http://www.jianshu.com/p/308f3c54abdd
https://news.realm.io/cn/news/droidcon-jake-wharton-simple-http-retrofit-2/