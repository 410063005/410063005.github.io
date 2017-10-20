---
title: Retrofit如何支持TCP
tags:
  - android
date: 2017-08-09 16:25:55
---
本文介绍了如何让Retrofit支持TCP接口
<!--more-->
# 背景
[Retrofit][github]号称是"Type-safe HTTP client for Android and Java"，它使用Java接口来定义HTTP API，并且支持JSON、Protobuf、XML等各种数据格式 ，使用非常方便。

实践中，我们的项目后台接口由原有的TCP接口 + Protobuf协议切换到HTTP接口 + JSON协议之后，Android客户端相应地引入Retrofit，大大地简化了接口访问代码的开发工作。配合使用[Postman][chrome postman](Postman独立版本见[这里][postman app])，原来让人抓狂容易扯皮的接口联调过程变得轻松愉快。

![postman截图](chrome_2017-08-09_10-42-23.png)

最近加入到另一个项目，发现后台接口也是TCP接口 + Protobuf协议，于是接口联调过程又回到以前状态，变得较为困难：一是PB二进制数据不可读，且难以像JSON文本数据一样可快速手工构造；二是TCP协议上进行私有加解密，导致没有类似Postman现成可用的接口测试工具。

校园项目有过推倒重来的阶段，由原有的TCP接口 + Protobuf协议切换到HTTP接口 + JSON协议时没有任何包袱换和顾虑。而这个项目后台、iOS终端、Android终端仍在快速迭代，切换后台接口工作量大，可能导致较多不稳定。

换个思路，我们能否做以下工作呢？

1. 让Retrofit支持TCP接口
2. 像Postman测试HTTP接口一样方便地测试TCP接口

本文尝试解决这里的第一个问题。主要内容包括Retrofit介绍，工作原理分析，然后讨论了如何让Retrofit支持TCP接口，以及如何实现自定义Converter。

# Retrofit简介

> Type-safe HTTP client for Android and Java by Square, Inc.

[Retrofit][github]是Android和Java平台的类型安全的HTTP客户端。还不够具体？接下来看

> Retrofit adapts a Java interface to HTTP calls by using annotations on the declared methods to define how requests are made. Create instances using the builder and pass your interface to create to generate an implementation.

Retrofit中使用注解来描述HTTP请求，动态代理生成可以发起相应HTTP请求的Java接口。举个例子

```java
public interface GitHubService {
  @GET("users/{user}/repos")
  Call<List<Repo>> listRepos(@Path("user") String user);
}
```

```java
Retrofit retrofit = new Retrofit.Builder()
   .baseUrl("https://api.github.com/")
   .addConverterFactory(GsonConverterFactory.create())
   .build();

GitHubService api = retrofit.create(GitHubService.class);
Response<List<Repo>> user = api.listRepos("张三").execute();
```

`Retrofit`负责生成`GitHubService`接口的具体实现。我们只管调用，不必手写后台接口访问代码，够简单吧。Retrofit是如何做到的呢？

*作者评论 ：其实原本就应该这么简单！ 想想看，接口访问代码难道多数不是样板代码？很多时候你不过复制粘贴，然后修改下确保参数正确而已。 *

# 工作原理
看下Retrofit工作原理。Retrofit包含以下关键类：

+ **Retrofit** - 它是整个模块的管理者，采用Builder模式。Retrofit可以将不同的`Converter.Factory`, `CallAdapter.Factory`, `Call.Factory`组合起来
+ **Converter** - 负责对象到HTTP以及HTTP到对象的转换。回想下，我们是不是经常在做数据转换，比如你通过HTTP接口从后台拉取一条数据，然后将HTTP响应体转换成需要的对象，这就是所谓的Converter
+ **Converter.Factory** - Converter工厂
+ **Call** - 表示一个准备执行的请求。准确地说，Call是OkHttp的接口(Retrofit 2依赖OkHttp)。Call接口规定：它可以被cancel，它代表单独的一对请求和响应，所以不能多次执行
+ **Call.Factory** - Call工厂。Call工厂是我们让Retroifit支持TCP接口的关键
+ **CallAdapter** - 不同于Converter，CallAdapter相对就不那么容易理解。简单来说，Retrofit接口不仅仅可以返回Call，也可以将Call适配成`AsyncTask`、`Future`、RxJava的`Observable`, 或其他的任何支持异步操作的对象，只要提供了相应的CallAdapter
+ **CallAdapter.Factory** - CallAdapter工厂
+ **ServiceMethod** - 与上面几个类不同，ServiceMethod不是公开的。只有`toRequest()`和`toResponse()`两个方法。  ServiceMethod也是Builder模式，ServiceMethod.Builder主要方法包括
 + `ServiceMethod.Builder.createCallAdapter()`
 + `ServiceMethod.Builder.createResponseConverter()`
 + `ServiceMethod.Builder.parseParameterAnnotation()`
 
Retrofit的原理是使用动态代理依据注解生成需要的代码，关键步骤在于`Retrofit.create()`：

```java
public <T> T create(final Class<T> service) {
    ...
    return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] { service },
        new InvocationHandler() {
          private final Platform platform = Platform.get();

          @Override public Object invoke(Object proxy, Method method, Object... args)
              throws Throwable {
            ...
            
            ServiceMethod serviceMethod = loadServiceMethod(method);
            OkHttpCall okHttpCall = new OkHttpCall<>(serviceMethod, args);
            return serviceMethod.callAdapter.adapt(okHttpCall);
          }
        });
}
```

坦白地说，原理你绝对都懂。不过Java中反射、泛型、注解等编码工作较为繁琐，另外Retrofit源码中参数检查、异常处理、调试信息、bug规避等代码占了相当大篇幅，抛开这些，核心代码其实很容易看明白，所以本文就不展开。[Retrofit原理浅析](http://www.jianshu.com/p/cd69c75d053e)中有较为清晰的分析，可以参考。

# 支持TCP
如何让Retrofit支持TCP？一开始的想法是修改源码不就行了。但修改源码会给后续工作带来很多不便，比如代码维护、项目构建、Retrofit库升级等等。

Retrofit支持HTTP，而HTTP是基于TCP的。实际上HTTP虽然是应用层协议，使用起来感觉比TCP简单多了，但其实现应该不会比TCP更简单。从这个层面来讲，能实现更复杂的功能，不可能搞不定简单的功能，对不对？ (看源码，其实OkHttp内部不仅实现了TCP连接，还有完善的TCP连接池)

上一节讲到`Call.Factory`是让Retrofit支持TCP的关键。使用Builder模式构适Retrofit时，除了使用最基本的`Builder.client(OkHttpClient client)`方式给Builder塞进一个OkHttpClient对象，还可以使用`callFactory`方法。实际上以下两个方法作用类似，都是设置Call.Fractory：

+ `Builder.client(OkHttpClient client)`
+ `Builder callFactory(okhttp3.Call.Factory factory)`

后者是更通用的形式，前者只是一个特例。是的，`OkHttpClient`也是一个`Call.Factory`，代码为证。

![OkHttpClient](okhttpclient.jpg)

明白了吧，只要我们实现`Call.Factory`接口，就可以基于`HttpURLConnection`写一个"KoHttpClient"，或是基于Apache HttpClient写一个"NotOkHttpClient"，然后替换Retrofit缺省依赖的OkHttpClient。所谓解耦或是扩展性，说的也许就是这个。那`Call.Factory`到底何方神圣？

```java
package okhttp3;
public interface Call extends Cloneable {
    Request request();
    Response execute() throws IOException;
    void enqueue(Callback responseCallback);
    void cancel();
    boolean isExecuted();
    boolean isCanceled();
    Call clone();
    
  interface Factory {
    Call newCall(Request request);
  }    
}
```

是不是简单得出乎你的意料。注意，OkHttp并规定Call必须是HTTP Call而不能是TCP Call。那好吧，我们来实现一个`TcpCall`以及`TcpCallFactory`：

```java
public class TcpCallFactory implements Call.Factory {
    public TcpCallFactory(String host, int port) {
        ...
    }
    
    @Override
    public Call newCall(Request request) {
        return new TcpCall(this, request);
    }
    
    static class TcpCall implements Call {
        
        @Override
        public Response execute() throws IOException {
            ...
        }
        
        @Override
        public void enqueue(Callback responseCallback) {
            ...
        }
    }
}
```

我们的项目中有现成的TcpClient，最终`TcpCall`是基于它来实现的。如果你没有直接可用的TcpClient，不妨看看`okhttp3.internal.io.RealConnection`源码，或许用得上。

`Call`同时支持同步请求和异步请求，见[Retrofit 2.0：有史以来最大的改进][retrofit2] ([翻译][retrofit2中文])，对应的方法分别为`execute()`和`enqueue()`。前者如何实现非常直观，而后者的实现则有一定技巧。具体代码可以参考[okhttp3.Dispatcher](https://github.com/square/okhttp/blob/master/okhttp/src/main/java/okhttp3/Dispatcher.java)源码。

另一个小细节就是`Call.execute()`的返回值，只要没有`IOException`异常，我们永远返回如下对象：

```java
new Response.Builder()
    .protocol(Protocol.HTTP_1_1)
    .code(200)
    .message("OK")
    .request(originalRequest)
    .body(ResponseBody.create(null, rsp))
    .build();
```

最后看看如何创建一个使用`TcpCallFactory`发送请求的Retrofit实例：

```java
Retrofit retrofit = new Retrofit.Builder()
    // 我们访问tcp接口，所以这行代码无实际意义
    // 仅仅是保证能通过retrofit内部参数检查
    .baseUrl("http://localhost:4000")
    .callFactory(new TcpCallFactory(host, port))
    .build();
```

# 自定义Converter
Retrofit中`Convert`是接口，具体如下：

```java
public interface Converter<F, T> {
    T convert(F value) throws IOException;
    
    abstract class Factory {  
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
            Retrofit retrofit) {
          return null;
        }    
        
        public Converter<?, RequestBody> requestBodyConverter(Type type,
            Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
          return null;
        }
        
        public Converter<?, String> stringConverter(Type type, Annotation[] annotations,
            Retrofit retrofit) {
          return null;
        }
    }
}  
```

Retrofit以独立模块的形式提供了几种[常用格式的Converter](https://github.com/square/retrofit/tree/master/retrofit-converters)。

上一节中我们已经让Retrofit支持通过TCP收发数据了。但TCP是传输层协议，如何在输入输出流中确定一条二进制消息的开始和结束，还需要自定义格式才行。所以客户端通过TCP接口访问后台并不是简单地使用标准Protobuf协议发送和接收数据，不能直接使用[Wire Converter][Wire Converter]。

我们的消息格式大致是这样：

|消息长度len|命令字cmd|消息体body|
|-----------|---------|----------|
|     4字节 |   4字节 | 不定长，PB |

请求消息 

|消息长度len| 错误码error | 消息体body |
|-----------|---------|----------|
|     4字节 |   4字节 |不定长，PB |

响应消息

*注意：请求消息中的消息体并不是必须的，某些查询请求就没有消息体*

<!-- TODO 图示 -->

需要根据消息格式实现自定义Converter。先看看[Wire Converter][Wire Converter]，它的两个Converter功能分别如下

+ WireRequestBodyConverter - Message对象转换为字节流(okhttp3.RequestBody)
+ WireResponseBodyConverter - 字节流(okhttp3.ResponseBody)转换为Message对象

HTTP中url本身就是命名良好的命令字，而响应码可以作为错误码，所以[Wire Converter][Wire Converter]用于HTTP接口数据转换时并不用关心命令字和错误码的问题。但就TCP接口而言，数据转换时需要关心命令字和错误码。设计如下：

```java
// 带命令字的请求
class CmdRequest {
    int cmd;
    Message message;
}

// 带错误码的响应
class StatusResponse<T extends Message> {
    int error;
    T message;
}
```

Custom Wire Converter与[Wire Converter][Wire Converter]差异如下：

+ CustomWireRequestBodyConverter - CmdRequest对象转换为字节流(okhttp3.RequestBody)
+ CustomWireResponseBodyConverter - 字节流(okhttp3.ResponseBody)转换为StatusResponse对象

剩下的就是一些具体的编码细节了，这里不过多展开。

# 总结
最后给出一个完整的用法，基本上跟添加TCP支持前的[Retrofit用法](http://square.github.io/retrofit/)完全一致：

```java
// AddressService.java
public interface AddressService {
    // 固定写法，有@Body参数时为'@POST("/")'，无@Body参数时为'@GET("/")'
    @POST("/")
    Call<StatusResponse<SetUserAddressRsp>> modifyAddress(@Body CmdRequest message);
}

// Demo.java
public void aDemo() {
    Retrofit retrofit = new Retrofit.Builder()
        // 我们访问tcp接口，所以这行代码无实际意义
        // 仅仅是保证能通过retrofit内部参数检查    
        .baseUrl("http://localhost:4000")
        .callFactory(new TcpClient(Env.getHostAddr(), Env.getHostPort()))
        .addConverterFactory(CustomWireConverterFactory.create(mRetrofitLogic.context()))
        .build();
    // 获取service实例
    AddressService addressService = retrofit.create(AddressService.class);
    // 创建修改地址请求
    SetUserAddressReq setUserAddressReq = ...
    // 创建请求参数
    CmdRequest cmdMessage = ...
    // 获取call对象
    Call<StatusResponse<SetUserAddressRsp>> call = addressService.modifyAddress(cmdMessage);
    call.enqueue(callback);
}
```

添加RxJava依赖之后，你还可以这么写，是不是有种很潮的感觉？

```java
// AddressService.java
public interface AddressService {
    // 固定写法，有@Body参数时为'@POST("/")'，无@Body参数时为'@GET("/")'
    @POST("/")
    Observable<StatusResponse<SetUserAddressRsp>> modifyAddress2(@Body CmdRequest message);
}

// Demo.java
public void aDemo() {
    Retrofit retrofit = ...
    // 获取service实例
    AddressService addressService = retrofit.create(AddressService.class);
    // 创建修改地址请求
    SetUserAddressReq setUserAddressReq = ...
    // 创建请求参数
    CmdRequest cmdMessage = ...
    // 获取call对象
    Observable<StatusResponse<SetUserAddressRsp>> observable = addressService.modifyAddress2(cmdMessage);
    observable.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(...);
}
```

*支持rxjava需要添加以下依赖*

```
compile 'io.reactivex:rxjava:1.1.6'
compile 'com.squareup.retrofit2:adapter-rxjava:2.0.0'
compile 'io.reactivex:rxandroid:1.2.1'
```

<!--

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


http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0915/3460.html
http://www.jianshu.com/p/308f3c54abdd
https://news.realm.io/cn/news/droidcon-jake-wharton-simple-http-retrofit-2/
-->

[Wire Converter]: https://github.com/square/retrofit/tree/master/retrofit-converters/wire
[retrofit2中文]: http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0915/3460.html
[retrofit2]: https://inthecheesefactory.com/blog/retrofit-2.0/en
[github]: https://github.com/square/retrofit
[chrome postman]: https://chrome.google.com/webstore/detail/postman/fhbjgbiflinjbdggehcddcbncdddomop?hl=zh-CN
[postman app]: https://www.getpostman.com/