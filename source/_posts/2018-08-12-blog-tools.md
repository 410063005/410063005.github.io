---
title: 一个不错的Hexo主题
date: 2018-08-12 21:08:41
tags:
---

最近又折腾了一下博客的主题以及HTTPS，记录一下。
<!--more-->
# next主题
首先是关于博客主题。上周看到一个超级对眼的主题，[next](https://github.com/iissnan/hexo-theme-next)，赶紧将自己的博客主题替换成next了。它很简单，看起来也舒服，简单让我有了更多动力写博客。

# https

## letsencrypt
Chrome现在直接将HTTP网站标记为不安全，所以我的博客也不得不支持HTTPS。原本抱着简单省事的心态打算上阿里云自己买一个，发现价格居然高达几千。作为个人博客，当然没必要买这么贵的啦。

那就找免费的吧。[Let's encrypt](https://community.letsencrypt.org/)是大家推荐得最多的免费SSL证书提供者。一方面是，很多第三方web托管服务支持[Let's encrypt](https://community.letsencrypt.org/)，见[这里](https://community.letsencrypt.org/t/web-hosting-who-support-lets-encrypt/6920)，当然，其中包括我们熟知的Github Pages。我自己的博客也是直接放Github。另一方面，letsencrypt提供了非常便捷的自动化工具[certbot](https://certbot.eff.org/)来简化申请HTTPS证书的过程，降低使用门槛。letsencrypt有90天的使用期限，到期后必须续期才能继续使用，这一点稍显不便。不过，对于免费的东西我们要求也不能过高。

这里有几篇关于letsencrypt使用参考：

+ [让网站永久拥有HTTPS - 申请免费SSL证书并自动续期](https://blog.csdn.net/xs18952904/article/details/79262646)
+ [Let's Encrypt 给网站加 HTTPS 完全指南](https://blog.csdn.net/andylau00j/article/details/54603975)

## Github Pages
Github Pages一直在改进(不得不赞)。Github Pages在2009年时就开始支持自定义域名，2016年开始`*.github.io`域名开始支持HTTPS，2018年5月1日自定义域名也开始支持HTTPS。[参考](https://blog.github.com/2018-05-01-github-pages-custom-domains-https/)。

所以托管在Github博客想要支持HTTPS其实很简单。

首先，为你的站点勾选“Enforce HTTPS”

![](enforce-https.png)

然后，更新A记录

185.199.108.153
185.199.109.153
185.199.110.153
185.199.111.153

两步即让你的博客支持HTTPS了，简单吧。据称，这几个新的IP不仅能让你的博客支持HTTPS，而且由于它们提供CDN功能，所以博客访问速度会变快。另外，还提供保护避免DDoS攻击。

mixed content的问题。现在你的博客由HTTP访问切换到HTTPS了，但博客中免不了可能有HTTP链接资源，如图片，CSS文件，Javascript文件等等。所以你的站点变成了mixed content站点，加载资源时可能失败导致页面解析问题。解决办法就是将HTTP链接修改成HTTPS链接。

# 优化

[hexo定制&优化 - 简书](https://www.jianshu.com/p/3884e5cb63e5)

优化方法是使用gulp对各种资源进行压缩。


```
hexo clean    //清除public文件夹
hexo g     //编译文章，生成public文件夹
gulp build    //压缩js、css、img文件
hexo d    //部署到github
```

[hexo定制&优化 - 简书](https://www.jianshu.com/p/3884e5cb63e5)

[hexo博客－性能优化 - luckykun - 博客园](https://www.cnblogs.com/jarson-7426/p/5660424.html)

[Hexo折腾记——性能优化篇-博客-云栖社区-阿里云](https://yq.aliyun.com/articles/8608)

# 参考
+ [GitHub Pages自定义域名如何支持https - CSDN博客](https://blog.csdn.net/Cowry5/article/details/80310052)
+ [如何添加A记录 Setting up an apex domain - User Documentation](https://help.github.com/articles/setting-up-an-apex-domain/)
+ [让个人域名下GithubPage完美支持https - CSDN博客](https://blog.csdn.net/u011244202/article/details/57106544/)
+ [Securing your GitHub Pages site with HTTPS - User Documentation](https://help.github.com/articles/securing-your-github-pages-site-with-https/)