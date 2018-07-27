---
title: glide-start
tags:
---


牛逼的不是那些细节，而是整体上的设计
不要过早地一头扎进细节出不来，会被各种decoder, model, model loader弄得很晕
建议先有基本的概念，然后整体理解
关键流程
 + 看日志, trace,
  + 关键日志整理
  + trace整理
 + 基本概念, 关键类， 类之间如何联系
  + 术语大全, target, model, modle loader, resource, resource decoder, data fetcher, generator, tracker, registry, encoder, engine, engine job, decode job,  trancode
  + A依赖B
  + A回调B
 + 生命周期绑定, 请求管理
 + 注册机制, builder机制
 + 缓存和对象池
 + 源码精读, 
  + GlideModule, Registry,   官方demo
  + GlideBuilder， 缓存, 对象池,  缓存策略是否合理? 
  + SourceGenerator/

  + RequestManagerFragment/SupportRequestManagerFragment
  + RequestManager/RequestManagerRetriever/RequestTracker
  + Engine/DecodeJob/SingleRequest
  + HttpGlideUrlLoader/HttpUrlFetcher,  官方demo
  + 
  + StreamBitmapDecoder, Downsampler
 + 最后再去研究那此细节吧。为什么要自己设计n多数据结构，为什么不使用系统的

最后从另一个视角来看glide
 很多可学习的地方

