---
title: cc常见问题
tags:
---


1. 可以作为static inner class，但没有加static
2. 没有用到的代码被扫出警告
3. 某些Android要求必须调用super方法的地方没有调用(主要是Activity的生命周期方法)
4. 可以作为static字段，但没有加static
5. 资源未关闭
6. 访问不到的代码
7. 不再被使用的类
8. makes inefficient use of keySet iterator instead of entrySet iterator.
9. 字符串跟byte[]转换时没有指定编码