---
title: use-hexo
tags:
---
原本是使用octpress的，但是一直没有找到漂亮的主题，再加上几次编译失败又找不到原因后就放弃了。

后来尝试过farbox，付费的。勉强还行，自动上传图片很方便。但有两个问题：1. 官方提供的主题总是觉得有一点丑 2. FarboxEditer用起来并不顺手。

发现hexo非常不错，见过好几个人使用，而且主题都很漂亮(这一点特别吸引不会前端开发、懒得折腾的我)。于是最近试用了一下。

公司Windows 7上安装hexo比较顺利。家里的Windows 10上则遇到各种坑。一个坑是npm被墙而报错信息又不明确。

找到[这篇文章](http://www.jianshu.com/p/31eb84182156)，解决办法是安装`cnpm`来代替`npm`

```
npm install -g cnpm --registry=https://registry.npm.taobao.org
```

解决npm问题后安装使用都非常方便。几个常用的命令如下：

+ `hexo new draft <title>` - 新建草稿
+ `hexo generate` - 编译生成静态文件
+ `hexo server` - 启动服务器
+ `hexo deploy` - 部署
