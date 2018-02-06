---
title: Android项目构建环境优化 
date: 2018-02-01 18:14:39
tags:
 - Android
---

[TODO] 我不知道标题名是否不准确。个人理解Android项目构建环境同样重要，也需要不断清理和优化，保证快的编译速度，提高开发效率。

<!--more-->

# debugCompile的使用
我们经常使用`compile`来添加依赖。但是有些依赖在debug包中才使用，而在release包中完全没有必要。这时其实使用`debugCompile`就足够了。

