---
title: 如何在ActionBar上显示引导提示
tags:
  - Android
toc: true
date: 2018-01-11 21:04:51
---
你一定在微信朋友圈见过弹出来指向右上角的箭头，"诱导"你去分享。通常来说，箭头只能指向右上角，没法指"进"右上角。有没有办法实现呢？微信中h5应该是没有办法的，但app自己的原生代码中肯定是可以实现的。来看看怎么做的吧。

<!--more-->

# 问题
给出如下视觉，要求ActionBar的Menu上能弹出一个提示：

![](/images/1515671077336.png)

内心的感受是~!^%$#@!!!#))*

但是，抛开设计或需求上的不合理性不谈，那到底如何实现这里的提示呢？要知道，它可是跑到ActionBar上面去了啊。

# 解决方案
实际项目中，一般都会定义BaseActivity用于统一控制页面结构。比如我们项目的BaseActivity结构如下：

```
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/common_base_activity_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <android.support.v7.widget.Toolbar
        android:id="@+id/common_base_activity_toolbar"
        android:layout_width="fill_parent"
        android:layout_height="50dp" />

</LinearLayout>
```

`common_base_activity_toolbar`作为ActionBar的容器，会加载如下View作为真正的ActionBar。

![](/images/1515673781733.png)

## 方案一

考虑到BaseActivity，一种明显的解决办法是绕开BaseActivity的布局结构，另起炉灶。由于一切从头开始，所以上述实际上述视觉效果并不存在太大困难。

## 方案二

Android设置android:clipChildren达到的特殊UI设计效果，这里是一个[例子](http://blog.csdn.net/zhangphil/article/details/48655411)。简单尝试后失败，可能的原因如下：

+ 要让`andorid:clipChildren`属性生效，必须设置在准确的View上，而我们的布局比例子中要复杂，这个准确的节点应该是位置BaseActivity的布局中
+ 只有固定大小的View才可以超出父View (?待求证)

## 方案三

我们在BaseActivity的布局中提供的`LinearLayout`并不是页面真正的根节点，真正的根节点是Android系统添加的`FrameLayout`。

通过`findViewById(android.R.id.content)`不难找到这个`FrameLayout`，我们可以将前面说到的"ActionBar的Menu上弹出的提示"添加到`FrameLayout`上，实现要求的视觉效果。关键代码如下：

```java
    void showHint(int topMargin, int layoutId) {
		// 获取FrameLayout
        ViewGroup activityContent = (ViewGroup) findViewById(android.R.id.content);
		// 生成hint
        View hintView = getLayoutInflater().inflate(layoutId, mActivityContent, false);
		// 调整布局参数
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) hintView.getLayoutParams();
        if (params != null) {
            params.topMargin = topMargin;
            hintView.setLayoutParams(params);
        }
		// 添加并显示hint
        activityContent.addView(hintView);
    }
```

在Layout Inspector可以比较直观地看到上述代码对布局的修改。

![](/images/1515672774540.png)

## 方案四

我还脑洞大开地想出了这个方案，

![](/images/1515674263401.png)

![](/images/1515674523010.png)

弹出提示和没有弹出提示时分别使用不同的图标，通过"欺骗"用户的方式实现最终效果。

# 总结
方案三以比较简单低成本的方式实现了需求。

暂时没想到更好的实现方法。如果你有，可以告诉我。