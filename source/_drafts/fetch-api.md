---
title: fetch_api
tags:
---



想在js中访问我们后台的api。使用[http-client][http-client]尝试了下。发现返回错误

```
OPTIONS https://test.campusx.qq.com/api/v1/configs 403 (Forbidden)
1.html:1 Fetch API cannot load https://test.campusx.qq.com/api/v1/configs. Response to preflight request doesn't pass access control check: No 'Access-Control-Allow-Origin' header is present on the requested resource. Origin 'null' is therefore not allowed access. The response had HTTP status code 403. If an opaque response serves your needs, set the request's mode to 'no-cors' to fetch the resource with CORS disabled.
1.html:1 Uncaught (in promise) TypeError: Failed to fetch
```

# 什么是CORS
1. 什么是cors
2. 什么是non-simple request
[来源][cors]

# 什么是opaque request
[来源][opaque]

> Consider the case in which a service worker acts as an agnostic cache. Your only goal is serve the same resources you would get from network but faster. Of course you can't ensure all the resources will be part of your origin (consider libraries served from CDNs, for instance). As the service worker has the potential of altering network responses, you need to guarantee you are not interested on the contents of the response, nor on its headers, nor even on the result. You're only interested on the response as a black box to possibly cache it serve it faster.

This is what { mode: 'no-cors' } was made for

> Opaque responses can't be accessed by JavaScript, but you can still cache them with the Cache API and respond with them in the fetch event handler in a service worker. So they're useful for making your app offline, also for resources that you can't control (e.g. resources on a CDN that doesn't set the CORS headers).

# 禁用chrome安全策略
[来源][disable-security-policy]

For Windows users:

The problem with the solution accepted here, in my opinion is that if you already have Chrome open and try to run this it won't work.

However, when researching this, I came across a post on Super User, Is it possible to run Chrome with and without web security at the same time?.

Basically, by running the following command (or creating a shortcut with it and opening Chrome through that)

```
chrome.exe --user-data-dir="C:/Chrome dev session" --disable-web-security
```

you can open a new "unsecure" instance of Chrome at the same time as you keep your other "secure" browser instances open and working as normal.

可以给`"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe" --user-data-dir="C:/Chrome dev session" --disable-web-security`建立桌面快捷方式，能快速启动禁用安全策略的chrome浏览器，方便开发。 [关闭安全策略](http://www.cnblogs.com/zhongxia/p/5416024.html)


这个[插件][plugin]也可以实现类似功能。

[http-client]: https://github.com/mjackson/http-client
[cors]: http://stackoverflow.com/questions/10636611/how-does-access-control-allow-origin-header-work
[opaque]: http://stackoverflow.com/questions/36292537/what-is-an-opaque-request-and-what-it-serves-for
[disable-security-policy]: http://stackoverflow.com/questions/3102819/disable-same-origin-policy-in-chrome
[plugin]: https://chrome.google.com/webstore/detail/allow-control-allow-origi/nlfbmbojpeacfghkpbjhddihlkkiljbi?hl=zh-CN