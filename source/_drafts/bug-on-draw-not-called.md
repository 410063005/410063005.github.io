---
title: bug-on-draw-not-called
tags:
  - android
  - bug
---

> 之前有遇到invalidate()后onDraw()不被调用的问题没有？
> 看了下，requestLayout会做measure，layout，draw三步，invalidate只做draw，如果一开始measure宽高都为0，那么invalidate后还是0

自己写了个控件, 继承自`TextView`。发现调用`invalidate()`后`onDraw()`并没有被回调。不太理解。


分析：自定义的控件，`setNumber()`后直接`invalidate()`是不行的，因为只会导致重绘。但实际上`setNumber()`后控件大小必须改变以容纳内容。所以其实应该是调用`requestLayout()`

# View.invalidate()解析



[ref]: http://www.jianshu.com/p/effe9b4333de