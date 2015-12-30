---
layout: post
title: "getItemViewType的用法"
date: 2015-12-30 14:48:06 +0800
comments: true
categories: android
published: true
---
本文介绍了`getItemViewType()`方法的用法。
<!--more-->

`getViewTypeCount()`方法和`getItemViewType()`方法用于处理ListView中出现不同类型View的情形。比如，在联系人应用中可能需要在偶数行的左边显示图片，奇数行的右边显示图片。这时可以使用以下代码：

```
@Override
public int getViewTypeCount() {
    return 2;
}

@Override
public int getItemViewType(int position) {
    return position % 2;
}
```

Android Framework使用`view type`来确定 **getView()方法中的`convertView`参数到底该传入什么样的View**。换句话说，上面的例子中，偶数行只会复用图片在左边的convertView，而奇数行只会复用图片在右边的convertView。

如果ListView中的每一行都使用相同的布局，不用考虑view type的问题。实际上[BaseAdapter.][BaseAdapter]为所有的adapter提供缺省的行为：

```
public int getItemViewType(int position) {
    return 0;
}

public int getViewTypeCount() {
    return 1;
}

```

实际上这是为每一行都提供相同的view。下面给出了常用的操作流：

1. 使用adapter绑定数据到`AdapterView`
2. `AdapterView`显示数据中可见的item
3. Android framework为第`n`行调用`getItemViewType()`方法，该行将被显示
4. Android framework从view池中为第`n`行挑出可复用的对象。一开始，没有对象可以被复用
5. 为第`n`行调用`getView()`方法
6. 为第`n`行调用`getItemViewType()`确定将使用什么类型的view
7. 使用if/switch根据view type决定使用哪种xml布局
8. 布局生成view
9. 返回上一步生成的view，退出`getView()`。显示生成的view

当ListView中的view滚动动屏幕外，它将进入由framework管理的view池以便后面重用。这些待重用的view按view type组织，以确保`getView()`方法中的`convertView`参数类型正确。

1. framework为将要显示的row再次调用`getItemViewType()`
2. 从view池中选择恰当类型的view
3. 上一步被选中的view作为`convertView`参数传到`getView()`方法
4. 在复用的view中填充新的数据并返回该view


# 总结
注意：

1. `getViewTypeCount()`方法要返回一共有几种view type
2. `getItemViewType()`方法的返回值的合法取值范围是0到 `getViewTypeCount() - 1`。另外，`Adapter.IGNORE_ITEM_VIEW_TYPE`也是合法的返回值


小技巧: 
> `Adapter.IGNORE_ITEM_VIEW_TYPE`是个特殊的view type，可用于不显示特定的item view。


```
    /**
     * An item view type that causes the {@link AdapterView} to ignore the item
     * view. For example, this can be used if the client does not want a
     * particular view to be given for conversion in
     * {@link #getView(int, View, ViewGroup)}.
     * 
     * @see #getItemViewType(int)
     * @see #getViewTypeCount()
     */
    static final int IGNORE_ITEM_VIEW_TYPE = AdapterView.ITEM_VIEW_TYPE_IGNORE;
```


[ref]: http://stackoverflow.com/questions/5300962/getviewtypecount-and-getitemviewtype-methods-of-arrayadapter
[BaseAdapter]: http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;f=core/java/android/widget/BaseAdapter.java;h=532fd766ec66ae54a6e4b3def4b8bdc839c1db7a;hb=refs/heads/master