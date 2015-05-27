---
layout: post
title: "我的效率工具列表"
date: 2015-05-27 10:40:57 +0800
comments: true
categories: tools
published: false
---
# 编辑器
## [Pandoc](http://pandoc.org/)
一个用于将Markdown文档转换成其他格式的命令工具。支持的格式包括HTML，EPUB，docx，PDF等等。Pandoc是用Haskell编写的(貌似非常小众的编程语言)，[源码](https://github.com/jgm/pandoc)托管在Github上。Pandoc使用UTF-8编码方式来处理输入和输出。注意，像HTML之类的文档会在头部加上字符编码信息，只有使用`-s/--standalone`选项时才会生成这些头部信息。

一些有用选项：

+ `--toc` - 生成Table Of Content目录列表
+ `-s` - 独立模式
+ ` -c <css-file>` - 指定CSS样式表

> Pandoc uses the UTF-8 character encoding for both input and output. If your local character encoding is not UTF-8, you should pipe input and output through iconv:
> `iconv -t utf-8 input.txt | pandoc | iconv -f utf-8`
> Note that in some output formats (such as HTML, LaTeX, ConTeXt, RTF, OPML, DocBook, and Texinfo), information about the character encoding is included in the document header, which will only be included if you use the -s/--standalone option.

[引用来源](http://pandoc.org/README.html)

# 快捷键
## WinHotKey
可以在Windows上自定义各种快捷键打开应用或目录，非常方便

## Vimium
Chrome中的插件，快捷键的使用非常类似于Vim。习惯使用"J"和"K"翻页后，鼠标简直是多余的。

# 文档管理
## Everything
搜索本地文档非常方便，再也不用担心记不清文件放哪了

# 在线工具
[TinyPNG](https://tinypng.com/)可以用于压缩PNG图片的大小。
