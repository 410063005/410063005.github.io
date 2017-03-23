---
title: 如何跟人PK
tags:
---
今天跟外部开作时遇到一个问题。需求其实很简单，我们应用跟某网站合作，使用WebView打开他们网站。但正常浏览时网站时会它会首页会显示一个下载提示条。

![](download.jpg)

而我们的产品比较有洁癖，要求从应用中访问该网站时不要显示下载条。对方开发给出的方案时让我们修改WebView的user agent，修改后的user agent加入白名单，不显示下载条。

我很快改好了。在缺省的user agent后面加上应用名称和versionCode

```java
PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
String ua = webView.getSettings().getUserAgentString() + " PmdCampus/" + pi.versionCode;
webView.getSettings().setUserAgentString(ua);
```

以为就这么愉快地完成任务了。然而合作网站先是有排期问题相应的修改迟迟上不了线，好不容易等到我们应用提测那天他们上线了，但发现从应用中访问网站时仍然会出现下载条。

对方认为已经安排上线，仍然显示下载条问题在于我们，应该是user agent没有写对。并且提供了user agent的检查方式。

```javascript
// userAgent已经被转换为小写字母
window.isWechat = /micromessenger|pmdcampus/.test(userAgent)
```

怎么可能？好吧，PK开始了。当然，不是争吵，争吵没有意义。PK是为了回答以下几个问题：

+ 问题到底在哪一方
+ 问题是什么原因
+ 问题该如何解决

先理一下思路，

+ WebView的user agent是否按预设规则修改
+ 合作网站上检查user agent的正则表达式是否正确
+ 是否其他原因导致仍然显示下载条

# 检查user agent

Chrome中访问`chrome://inspect/#devices`，选择对应的页面，打开Console，刷新一下，观察请求中的user agent。符合预期。

![](453873450.jpg)

难道是对方的检查机制有问题，验证一下。发现没有任何问题。

![](reg.jpg)

看合作网站上的javascript代码，`window.isWechat`的值也确实为true

![](1181057691.jpg)

看来问题出在别的地方，由于不熟悉js，就没有进一步分析了

# 更多证据
如果iOS端修改user agent后访问该网站也是仍然显示下载条，应该能排除自己的问题。可惜iOS开发进度稍慢，没有帮我验证。不过观察以下两个现象，应该可以排除我方问题

1. 修改Chrome浏览器的user agent为`Mozilla/5.0 (Linux; Android 6.0; Google Nexus 5 - 6.0.0 - API 23 - 1080x1920 Build/MRA58K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36 PmdCampus-2.6.6.4 PmdCampus/20606004`，从Chrome访问合作网站，仍然有下载条
2. 按照检查user agent的正则式，微信中打开该网站也应该看不到下载条。从微信访问合作网站，仍然有下载条


[ref]: http://www.cnblogs.com/terrylin/p/4606277.html
