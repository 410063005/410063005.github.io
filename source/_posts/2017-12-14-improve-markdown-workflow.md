---
title: Markdown之贴图小技巧
toc: true
date: 2017-12-14 17:22:21
tags:
 - favorite
---

最近使用Markdown写博客比较勤，发现Markdown中贴图十分烦琐。而作为程序员又经常有博客中贴代码截图的需求，那么如何快速贴图呢？
<!--more-->
在你继续阅读前，我需要申明一下：

+ 我在Mac系统上使用Markdown写博客
+ 我的博客图片不使用第三方图床，而是直接放在GitHub

如果你的工作环境跟我不同，我的做法可能未必适合你！

# 常规贴图方式的问题
Mac上有两种截图方式。一是 `Control + Shift + Command + 4` 截图到剪贴板，二是 `Shift + Command + 4` 截图保存到桌面。

+ 第一种方式并不方便直接粘贴到Markdown文本中
+ 第二种方式是将截好的图片保存到桌面

我使用第二种方式截图。但为了把这张图片放在博客中，我必须手动将其移到博客中的图片文件夹，然后在博客中使用`![]()`来引用这张图片。想想你就知道有多繁琐，尤其连续给代码截图时会非常不便。难怪写博客效率低！

# 解决方案
解决方案是将剪切板中的图片保存到博客中指定的目录，并以`![]()`这种格式自动生成该图片的引用，并将这个引用写入剪切板。这样，我们就可以直接使用`Command + V`将图片引用复制到博客。听起来很方便一样，如何操作呢？我们先看有哪几个关键点：

+ 如何访问剪切板图片
+ 如何生成图片引用
+ 如何将图片引用写入剪切板
+ 如何自动化

第四点是最最重要的，我们会用到Mac自带的Automator来实现自动化。

## 如何访问剪切板图片
直接上代码，代码来自[简化markdown写作中的贴图流程 - 简书](http://www.jianshu.com/p/7bd4e6ed99be)。

```python
# -*- coding: utf-8 -*-
# clipboard2.py
import time
from AppKit import NSPasteboard, NSPasteboardTypePNG, NSPasteboardTypeTIFF
import os

BLOG = '/Users/kingcmchen/blog/source/images/'
def copy_to_blog(img_path):
    img_input = file(img_path, 'rb')
    name = os.path.basename(img_input.name)
    img_output = file(BLOG + name, 'wb')
    img_output.writelines(img_input.readlines())
    return '![](/images/' + name + ')' 

def get_paste_img_file():
    pb = NSPasteboard.generalPasteboard()
    data_type = pb.types()
    # if img file
    #print data_type
    now = int(time.time() * 1000) # used for filename
    if NSPasteboardTypePNG in data_type:
        # png
        data = pb.dataForType_(NSPasteboardTypePNG)
        filename = '%s.png' % now
        filepath = '/tmp/%s' % filename
        ret = data.writeToFile_atomically_(filepath, False)
        if ret:
            return filepath
    elif NSPasteboardTypeTIFF in data_type:
        # tiff
        data = pb.dataForType_(NSPasteboardTypeTIFF)
        filename = '%s.tiff' % now
        filepath = '/tmp/%s' % filename
        ret = data.writeToFile_atomically_(filepath, False)
        if ret:
            return filepath
    elif NSPasteboardTypeString in data_type:
        # string todo, recognise url of png & jpg
        pass

if __name__ == '__main__':
    #print(get_paste_img_file())
    try:
        print(copy_to_blog(get_paste_img_file()))
    except:
        print('')
```

代码含义如下：

+ `get_paste_img_file()` - 获取保存到剪贴板中的截图路径
+ `copy_to_blog()` - 将剪贴板截图保存到我的博客中的`images`目录下，并返回markdown格式的图片路径

<!--
# Alfred自动化
1. 下载安装[Alfred - Productivity App for Mac OS X](https://www.alfredapp.com/)
2. 解锁Alfred Workflows功能。好吧，这个花钱的，19美元
3. 

...今天没有带信用卡，搞不定...


http://www.jianshu.com/p/7bd4e6ed99be

https://www.zhihu.com/question/20656680

http://www.jianshu.com/p/e9f3352c785f

http://www.jianshu.com/p/0e78168da7ab
-->

## Automator自动化
原本打算使用Alfred自动化生成图片引用，但发现利用Mac自带的Automator也可改进贴图流程。只需要在Automator中配置好`clipboard2.py`这个脚本，我们可以非常便捷地向markdown贴图：

![](/images/1513241525096.webp)

完整的操作流程如下：

1. 先使用`Control + Shift + Command + 4`命令截图到剪贴板
2. 在Automator中点击"运行"，执行`clipboard2.py`脚本
3. 在Markdown文件中`Command + V`，以`![]()`这种格式粘贴刚才的截图

比手工操作方便快捷多了吧?