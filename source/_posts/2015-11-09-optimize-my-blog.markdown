---
layout: post
title: "优化我的博客"
date: 2015-11-09 19:34:19 +0800
comments: true
categories: other
published: false
---

博客托管在Github上，目前使用的是未经修改的[Octopress](http://octopress.org/)。访问非常缓慢。决定做些优化，记录如下。

![useso](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/useso.PNG)

```
http://fonts.useso.com/css?family=PT+Serif:regular,italic,bold,bolditalic
```

有两个请求超慢，Why? 搜索关键字`octopress fonts useso`，找到[这篇文章](http://devework.com/google-fonts-in-wordpress.html)

1. 原来是使用了[360网站卫士常用前端公共库CDN服务](http://libs.useso.com/)来替代Google Fonts (自己做的修改，时间久，忘记了)
2. 第一次加载字体库时非常慢，之后从cache中加载速度很快

这不是关键优化点，放弃

---

![twitter](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/twitter.PNG)

这么小的请求，耗时却这么久。分析下。参考[这个](http://droidyue.com/blog/2014/06/22/fix-octopress-slow-loading-speed-issue-in-china-mainland/)

去除Twitter按钮

---

有张255KB的大图片，下载时间2秒多。PNGoo压缩到141KB，更新。
