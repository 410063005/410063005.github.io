---
layout: post
title: "使用Octpress写博客"
date: 2015-05-14 15:12:48 +0800
comments: true
categories: other
---

今天试用Octpress，感觉非常不错。安装试用过程记录如下。操作非常简单，基本是按照帮助文档一路 Next，所以这里只记录遇到的问题以及跟帮助文档不符的地方(也许文档过期了)

# 1. 安装
分别要安装 Git，Ruby，DevKit，Python，最后才是 Octpress。

DevKit解压后，需要运行`ruby dk.rb init`初始化。初始化应检查config.yml中正确配置了Ruby的安装路径，若无可手动添加。之后运行`ruby dk.rb install`进行安装。

注意：如果没有正确安装DevKit，运行`bundle install`时出错。错误提示`Please update your PATH to include build tools or download the DevKit`

步骤总结如下：

	Install Git, Python, Ruby
	解压DevKit
	ruby dk.rb init
	ruby dk.rb install
	配置Path环境变量
	git clone git://github.com/imathis/octopress.git octopress
	更新gem sources
	gem install bundle
	(在octopress目录中)bundle install

## 1.1 版本问题
安装Ruby和DevKit容易出错，主要问题是版本不对。为减少版本出错可能，还是从[RubyInstaller](http://rubyinstaller.org/downloads/)下载Ruby和DevKit吧。一定要注意版本对应关系：操作系统是否64位、支持哪个版本的Ruby。

## 1.2 代理问题
公司上网要走代理，`gem` 也需要设置相应的代理，设置方式

```
set http-proxy=<http proxy>
```

## 1.3 HTTPS问题
可能遇到无法从 `https://rubygems.org/` 下载安装的问题。更新源的方式如下

```
gem sources -r https://rubygems.org/
gem sources -a http://rubygems.org/
gem sources -l
```

也可以尝试国内的淘宝镜像 `http://ruby.taobao.org/`，速度可能快点。

# 2. 使用
主要命令如下：

+ `rake preview` - 通过 `http://localhost:4000/` 进行预览
+ `rake new_post['my blog']` - 添加文章
+ `rake generate` - 生成文章。每次执行了添加博客的命令，或是修改了现有博客的内容后都应执行这个命令
+ `rake watch` - 检测文件变化,实时生成新内容
+ `rake deploy` - 将本地的内容发布到Github上

如果将本地内容发布到Github，可以参考[这里](http://octopress.org/docs/deploying/github/)

**不要忘记** 将源码提交

```
git add .
git commit -m 'your message'
git push origin source
```

# 3. 参考资料
1. [Windows上安装Octopress博客](http://www.open-open.com/lib/view/open1423539522764.html)
2. [Windows下搭建Octopress博客](http://www.cnblogs.com/oec2003/archive/2013/05/27/3100896.html)
3. [重装系统后恢复Octopress博客](http://ju.outofmemory.cn/entry/109363)