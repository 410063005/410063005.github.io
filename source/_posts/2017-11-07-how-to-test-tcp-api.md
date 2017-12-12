---
title: 如何联调TCP API 
date: 2017-11-07 16:12:44
tags:
 - Android
---

HTTP RESTful API是主流的API形式，易于测试。但一些系统当中使用TCP形式的接口，我们如何方便地对其进行测试呢？我的思路是将TCP接口适配成HTTP接口，然后用Postman等工具进行测试。本文详细描述了该适配方案的实现方案，以及实际测试中如何操作。

<!--more-->
