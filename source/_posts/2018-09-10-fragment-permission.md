---
title: Fragment权限问题的一个小坑
tags:
  - Android
date: 2018-09-10 17:35:13
---

记录Fragment中关于权限问题的一个小坑。
<!--more-->

调用`ActivityCompat.requestPermissions()`方法，`Activity.onRequestPermissionsResult()`回调正常，而`Fragment.onRequestPermissionsResult()`不回调。

StackOverflow上找了两篇帖子

+ [how-to-check-permission-in-fragment](https://stackoverflow.com/questions/40760625/how-to-check-permission-in-fragment)
+ [request-runtime-permissions-from-v4-fragment-and-have-callback-go-to-fragment](https://stackoverflow.com/questions/32890702/request-runtime-permissions-from-v4-fragment-and-have-callback-go-to-fragment)

给出的答案都是这样：

+ fragment v4 中需要使用 `Fragment.requestPermission(String[], int);`
+ 而Activity中需要使用 `AppCompat.requestPermission(Activity, String[], int)`

按给出的答案，将`ActivityCompat.requestPermissions()`修改成`Fragment.requestPermission(String[], int);`，`Activity.onRequestPermissionsResult()`及`Fragment.onRequestPermissionsResult()`都能正常回调。

这种设计还真是坑啊！

# 参考
[onRequestPermissionsResult not called on Fragments in Android – CODERZHEAVEN](http://www.coderzheaven.com/2016/10/12/onrequestpermissionsresult-not-called-on-fragments/)	




