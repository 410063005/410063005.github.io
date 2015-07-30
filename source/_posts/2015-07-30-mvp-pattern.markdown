---
layout: post
title: "(译)MVP模式跟MVC模式的差别"
date: 2015-07-30 10:44:23 +0800
comments: true
categories: java
---
本文主要介绍了MVP模式跟MVC模式的差别。学习以备忘。

<!--more-->

原文来自[这里](http://www.infragistics.com/community/blogs/todd_snyder/archive/2007/10/17/mvc-or-mvp-pattern-whats-the-difference.aspx)

这些年我指导过不少开发者使用设计模式和最佳实践。一个反复被问到的问题是：MVC和MVP模式之间的区别是什么？其实答案的复杂性超出你的想象。我想许多开发者不好意思使用这两种模式的部分原因正在于不理解它们之间的差异。

开始理解它们差异之前我们看看这些模式是如何工作的以及它们的好处。两种模式(MVC和MVP)都被使用了若干年，都强调了UI和业务层解耦这个主要的OO概念。现在有很多框架基于这两种模式，包括：Java Struts, ROR, CAB等等。

# MVC模式

![MVC](http://users.infragistics.com/tsnyder/MVC%20View.jpg)

MVC模式是一种UI表现层模式，强调将UI(View)从业务逻辑层(Model)中解耦。这种模式中三个组件承担自己的责任：view负责渲染UI元素，controller负责响应UI操作，而model负责业务行为和状态管理。大部分实现中这三种组件都能直接跟另外两种交互，而某些实现中controller负责确定显示某个view([Front Controller Pattern](http://msdn2.microsoft.com/en-us/library/ms978723.aspx))。

# MVP模式

![MVP](http://users.infragistics.com/tsnyder/MVP%20View.jpg)

MVP模式是一种基于MVC模式的UI表现层模式。这种模式将责任分离到四个组件中：view负责渲染UI元素，view interface用于将presenter跟view松耦合，presenter负责view/model间的交互，而model负责业务行为和状态管理。在一些实现中，presenter跟某个service层(controller)交互，以获取和持久化model。view interface和service layer通常用于让presenter和model的单元测试更容易编写。

# 优点
在使用任何模式之前开发者需要考虑它的优点和缺点。使用MVC和MVP模式有以下主要优点。但是，也有几个缺点应考虑。最大的坏处是额外的复杂性和学习曲线。而且这些模式也不适合简单的场景。

优点如下：

+ 松耦合 - presenter/controller是UI代码和model代码之间的中介。它允许view和model独立演进
+ 更清晰的关注点/责任
  + UI(Form和Page) - 负责渲染UI
  + Presenter/controller - 负责响应UI事件以及跟model交互
  + Model - 负责业务行为和状态管理
+ 测试驱动 -通过分离主要的组件(UI, Presenter/controller, model)，写单元测试更容易。使用MVP尤其如此，因为只使用接口跟view交互
+ 代码重用 - 通过关注点/责任分离，可以提高代码的复用度。使用full blown domain model并保证所有的业务/状态管理逻辑在恰当的地方，尤其可以提高重用
+ 隐藏数据访问 - 使用这些模式强迫你将数据访问代码放到数据访问层中。有其他几种模式跟MVC/MVP模式协同工作，用于数据访问。
+ 增加灵活性 - 通过分离大部分代码到presenter/controller和model组件，代码更容易修改。比如，考虑到这些年UI和数据访问技术的变化，今天我们有太多选择。正确使用MVC或MVP模式可以同时支持多种UI和数据访问技术

# 差异
那么MVC和MVP模式的真正差异是什么呢？事实上它们并不是完全不同。两种模式都强调多种组件责任分离，UI(view)跟业务逻辑(Model)松耦合。主要的不同在于模式的实现方式，某些场景下你同时需要presenter和controllerr。

两者主要差别如下：

+ MVP模式
 + View跟model更解耦。presenter用于绑定model到view
 + 通过接口访问view，所以更容易写单元测试
 + view跟presenter通常是一对一地映射。复杂的view可能有多个presenter
 
+ MVC模式
 + controller是基于行为的，可以在view间共享
 + 负责决定显示哪个view