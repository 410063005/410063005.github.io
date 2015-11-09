---
layout: post
title: "LinearLayout divider"
date: 2015-11-09 17:54:52 +0800
comments: true
categories: android
---
项目中某些地方实现的divider不尽合理，尝试做了些优化，总结如下。<!--more-->

我们知道，Android中实现divider的最简单方式是这样：

```
<View
	android:layout_width="match_parent"
	android:layout_height="1dp" android:background="#ff0000" />
```

截图如下：

![simple-divider](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/1.simple-divider.jpg)

非常简单是吧。这里并不要求一定是View，也可以是TextView或ImageView等等，无非是将控件的高度设置得非常小并添加相应的背景。但这种简单方式有时却不是"足够好用"。看以下场景。

# 改进前的方案
现在需要实现以下UI。

![original-ui](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/2.origin-ui.jpg)

布局代码可能类似这样：

```
<LinearLayout
	android:layout_width="match_parent"
	android:layout_height="wrap_content"
	android:orientation="vertical">

	<RelativeLayout
		android:layout_width="match_parent"
		android:layout_height="wrap_content">
		...
	</RelativeLayout>

	<!-- divider -->
	<ImageView style="@style/post_order_divider" />
	
	<RelativeLayout
		android:layout_width="match_parent"
		android:layout_height="wrap_content">
		...
	</RelativeLayout>

	<!-- divider -->	
	<ImageView style="@style/post_order_divider" />
	
	<RelativeLayout
		android:layout_width="match_parent"
		android:layout_height="wrap_content">
		...
	</RelativeLayout>

	<!-- divider -->	
	<ImageView style="@style/post_order_divider" />
	
	<RelativeLayout
		android:layout_width="match_parent"
		android:layout_height="wrap_content">
		...
	</RelativeLayout>	
</LinearLayout>
```
几天后又加了新需求，要求UI上可隐藏某些字段，比如说 *根据某些条件隐藏地址或期望完成时间* 。大概期望可以长这个样子。

![new-ui](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/3.new-ui.jpg)

只能这样实现：
```
// 隐藏地址及其下方的divider
View address = ...
View addressDivider = ...
address.setVisibility(View.GONE);
addressDivider.setVisibility(View.GONE);

// 隐藏期望完成时间及其下方的divider
...
```

没有什么大问题，只是不够"优雅"！我们关注点是怎么隐藏地址，真的不太care它下面的divider。理想情况下，关注点以外的东西最好不好牵扯的我们的精力。很忙滴好不好~

其实Android不只ListView支持divider属性，LinearLayout也支持divider属性(Android 3.0开始)，灵活运用LinearLayout的divider属性，可以有更简单的解决方案。继续看。

# 改进后的方案
LinearLayout添加divider，修改后的布局文件：

```
<LinearLayout
        android:divider="@drawable/list_divider"
        android:showDividers="middle"
	android:layout_width="match_parent"
	android:layout_height="wrap_content"
	android:orientation="vertical">

	<RelativeLayout
		android:layout_width="match_parent"
		android:layout_height="wrap_content">
		...
	</RelativeLayout>
	
	<RelativeLayout
		android:layout_width="match_parent"
		android:layout_height="wrap_content">
		...
	</RelativeLayout>

	<RelativeLayout
		android:layout_width="match_parent"
		android:layout_height="wrap_content">
		...
	</RelativeLayout>

	<RelativeLayout
		android:layout_width="match_parent"
		android:layout_height="wrap_content">
		...
	</RelativeLayout>	
</LinearLayout>
```

divider对应的drawable文件如下：

```
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="16dp"
    android:insetRight="16dp" >
 
    <shape>
        <size android:height="1dp"/>
        <solid android:color="#dcdcdc" />
    </shape>
 
</inset>
```

不但实现同样的分割线效果，额外的好处是可以非常灵活地隐藏LinearLayout的子节点。隐藏地址和期望完成时间的代码可以做些简化，看看是不是变清爽了：

```
View address = ...
address.setVisibility(View.GONE);

View deadline= ...
deadline.setVisibility(View.GONE);
```

下面是我们项目中另外一个可以使用LinearLayout简化代码的地方。视觉稿中从上到下分别是正文、人数、地点、时间 、支付，除正文外其他项都可能要求隐藏。

![replace-margin](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/4.replace-margin.jpg)

总体布局是这样的：
```
<LinearLayout>
	<TextView  />
	<LinearLayout  />
	<LinearLayout />
	<LinearLayout />
	<LinearLayout />
</LinearLayout>
```

为了在各项之间留空白，一种方式是给子节点设置margin或padding，另一种更好的方式是用前面讲到的LinearLayou的divider。divider如下:

```
<?xml version="1.0" encoding="utf-8"?>
<inset xmlns:android="http://schemas.android.com/apk/res/android"
    android:insetLeft="16dp"
    android:insetRight="16dp" >

    <shape>
        <size android:height="16dp"/>
        <solid android:color="#00000000" />
    </shape>

</inset>
```


相比而言：

1. 代码更清晰，不必给多个子节点设置margin
2. 不用担心潜在的空白过多的问题
3. 便于统一调整。万一哪天视觉觉得16dp的空白比8dp要好看呢

# 总结
实际项目中某些场景下我们不妨试试LinearLayout的divider来简化代码。它可能会给你带来以下好处：

1. 更少的子节点，更少的代码
2. 方便隐藏子节点
3. 易于统一调整

另外，Android 3.0开始LinearLayout才支持divider属性。如果要兼容3.0以下的设备，可以使用appcompat-v7库中的`android.support.v7.widget.LinearLayoutCompat`代替LinearLayout。

[参考](http://stackoverflow.com/questions/14054364/how-to-assign-padding-to-listview-item-divider-line)