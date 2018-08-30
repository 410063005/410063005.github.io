---
title: 记一次网页中文乱码问题
date: 2018-08-28 10:00:51
tags:
---
记一次网页中文乱码问题。问题产生的原因是Web服务器自动添加的Content-Type中的字符编码不正确。
<!--more-->
# 问题描述
Apache2中通过ProxyPass方式转发请求。

```
// apache2/conf/vhosts.d/xxx.conf
<IfModule mod_proxy.c>
    ProxyPass         /xmerger/  http://localhost:8888/
</IfModule>
```

Apache服务器上添加以上配置，将请求代理到8888端口的web服务。代理方式访问8888端口的web服务时出现乱码，而直接访问则不会有乱码。

一开始以为是HTML页面中没有指定正确的字符编码，检查后发现已指定charset为"utf-8"：

```html
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
</head>
</html>
```

使用Chrome开发者工具对比发现两种访问方式的区别在于HTTP的Response Header:

+ 代理方式访问(ProxyPass)时 `Content-Type: text/html; charset=GB18030`
+ 直接访问时 `Content-Type: text/plain;charset=utf-8`

推测乱码原因是因为Apache服务器修改了`Content-Type`。

`find . | xargs grep 'GB18030'`搜索后发现`apache2/conf/vhosts.d/xxx.conf`有如下配置：

```
AddDefaultCharset GB18030
```

# 解决办法
原来的ProxyPass配置如下：

```
<IfModule mod_proxy.c>
    ProxyPass         /xmerger/  http://localhost:8888/
</IfModule>
```

将其修改为：

```
<IfModule mod_proxy.c>
    <Location "/xmerger/">
        ProxyPass "http://localhost:8888/"
        AddDefaultCharset UTF-8
    </Location>
</IfModule>
```

重启Apache服务器，返回后通过代理方式访问也能收到正确的响应头：`Content-Type: text/html; charset=UTF-8`，中文乱码问题解决。

# 参考
[proxypass](https://httpd.apache.org/docs/2.4/mod/mod_proxy.html#proxypass)