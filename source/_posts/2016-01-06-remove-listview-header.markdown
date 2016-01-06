---
layout: post
title: "如何移除ListView的header"
date: 2016-01-06 12:02:06 +0800
comments: true
categories: android view
published: true
---
本文介绍了如何移除ListView的header。
<!--more-->

# 问题现象

> 从`listview`中移除header时遇到问题了。首先，我通过`addHeaderView()`添加header，当切换到另一个布局时我想用`removeHeaderView()`隐藏header时出现问题。它不能正常工作。将header view的可见性设置为`GONE`也不起作用。

# 问题分析

Android中`ListView.addHeaderView()`和`ListView.addFooterView()`方法非常奇怪：你必须在给ListView设置adapter前先调用这两个方法添加header和footer，否则方法调用会抛出异常。下面添加一个`ProgressBar`作为header：

```
// spinner is a ProgressBar
listView.addHeaderView(spinner);
```

你可能想显示或隐藏这里的spinner，但直接移除spinner是非常危险的，因为除非销毁该ListView否则没有办法再次给其添加header。另外再次强调，给ListView设置adapter后不能再调用`addHeaderView()`：

```
listView.removeHeaderView(spinner); //dangerous!
```

那么可否隐藏header呢。隐藏header也很困难。因为仅仅隐藏spinner会导致出现一个空的、但仍然可见的header区域。

![header](http://i.stack.imgur.com/DkWeV.jpg)

![empty visible header](http://i.stack.imgur.com/acCl0.jpg)

# 解决办法
解决办法是将这里的spinner放到`LinearLayout`中，并设置`android:layout_height="wrap_content"`。当`LinearLayout`的内容被隐藏时它会收缩，header这里其实仍然是可见的，但其高度为0：

```
<LinearLayout 
      xmlns:a="http://schemas.android.com/apk/res/android"
      android:orientation="vertical"
      android:layout_width="fill_parent"
      android:layout_height="wrap_content">
    <!-- simplified -->
      <ProgressBar
        android:id="@+id/spinner"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"/> 
  </LinearLayout>
```

将上面的布局设置为header：

```
spinnerLayout = getLayoutInflater().inflate(R.layout.header_view_spinner, null);
listView.addHeaderView(spinnerLayout);
```

当需要隐藏header时，不是直接隐藏spinnerLayout，而是隐藏其content：

```
spinnerLayout.findViewById(R.id.spinner).setVisibility(View.GONE);
```

这回，header终于隐藏了！


---
ListView的header设计是有问题的。 [TODO]

[ref]: http://stackoverflow.com/questions/13603888/remove-header-from-listview