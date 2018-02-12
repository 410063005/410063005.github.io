---
title: 如何写Postman测试脚本
tags:
toc: true
---

Postman中可以使用Javascript编写测试脚本。测试脚本会在收到响应后运行。下面是一个最简单的测试脚本

![](/images/1515575144261.png)

```javascript
tests["Status code is 200"] = responseCode.code === 200;
```

这个测试判断向http://www.baidu.com发送GET请求收到的状态码是否200。

中间 - 测试脚本代码
右侧 - 内置的代码片断，实现常用功能，比如"Status code is 200"
下侧 - 测试脚本代码运行结果



# 参考
1. [官网](https://www.getpostman.com/docs/postman/scripts/test_scripts)