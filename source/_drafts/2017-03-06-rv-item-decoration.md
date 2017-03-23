---
title: rv-item-decoration
date: 2017-03-06 10:06:38
tags:
 - android
---

`void addItemDecoration(ItemDecoration decor)` 给RecyclerView 添加 ItemDecoration。 Item decoration会影响单个item view的测量和绘制。

ItemDecoration允许为指定的item添加特殊的drawing and layout offset。要在item之间绘制divider，高亮显示item，或给一批item添加边框时ItemDecoration非常有用。

```java
public static abstract class ItemDecoration {

    // 在RecyclerView提供的Canvas上绘制decorations
    // 这个方法绘制的内容会在view之前绘制，所以它会显示在view的下方
    public void onDraw(Canvas c, RecyclerView parent, State state) {
            onDraw(c, parent);
    }
    
    // 在RecyclerView提供的Canvas上绘制decorations
    // 这个方法绘制的内容会在view之后绘制，所以它会显示在view的上方
    public void onDrawOver(Canvas c, RecyclerView parent, State state) {
        onDrawOver(c, parent);
    }
    
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
        getItemOffsets(outRect, ((LayoutParams) view.getLayoutParams()).getViewLayoutPosition(),
                parent);
    }    
}
```

```
03-05 22:42:38.707 30982-30982/? I/DividerItemDecoration: getItemOffsets: 
03-05 22:42:38.735 30982-30982/? I/DividerItemDecoration: onDraw: 
03-05 22:42:38.736 30982-30982/? I/DividerItemDecoration: onDrawOver: 
```

通过日志不难确定这三个方法的调用顺序。

# 原理分析

`getItemOffsets()`跟`onDraw`是如何交互的?



getItemDecorInsetsForChild() --> mItemDecorations.get(i).getItemOffsets()

`getItemDecorInsetsForChild()` 会调用 `ItemDecoration.getItemOffsets()` ，并将生成的结果保存到 child view 的 LayoutParams当中。

而`onDraw()` 中绘制时，可以调用 `RecyclerView.getDecoratedBoundsWithMargins()` 拿到已算好的 item offsets

# DividerItemDecoration
DividerItemDecoration是ItemDecoration的简单实现。可以用作LinearLayoutManager布局的item分割线。它支持水平分割线和垂直分割线。

![](divider_item_decoration.jpg)
![](divider_item_decoration2.jpg)

DividerItemDecoration是我们学习如何实现自己的ItemDecoration的好例子。

两个关键点：

+ 用作divider的drawable，它决定分割线的样式及高度
+ 布局方向，它决定分割线的方向

两个主要方法：

+ void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state)
+ void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)

DividerItemDecoration中用于divider的drawable来自`android.R.attr.listDivider`。而`android.R.attr.listDivider`是`attrs.xml`中的定义如下：

```xml
<resources>
    <!-- These are the standard attributes that make up a complete theme. -->
    <declare-styleable name="Theme">
        <!-- The drawable for the list divider. -->
        <attr name="listDivider" format="reference" />    
    </declare-styleable>
</resources>
```

`themes.xml`中指定了具体值

```xml
<!-- themes.xml -->
<resources>
    <style name="Theme">
        <item name="listDivider">@drawable/divider_horizontal_dark</item>
    </style>
    <style name="Theme.Light">
        <item name="listDivider">@drawable/divider_horizontal_bright</item>
    </style>
</resources>

<!-- themes_holo.xml -->
<resources>
    <style name="Theme.Holo">
        <item name="listDivider">@drawable/list_divider_holo_dark</item>
    </style>
    <style name="Theme.Holo.Light" parent="Theme.Light">
        <item name="listDivider">@drawable/list_divider_holo_light</item>
    </style>
</resources>

<!-- themes_material.xml -->
<resources>
    <style name="Theme.Material">
        <item name="listDivider">@drawable/list_divider_material</item>
    </style>
    <style name="Theme.Material.Light" parent="Theme.Light">
        <item name="listDivider">@drawable/list_divider_material</item>
    </style>
</resources>
```

可以看到在不同版本的Android上，divider有所不同。即使相同版本，也会有dark和light之分。而在Material主题中`list_divider_material`是xml而不是点九图

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
       android:tint="?attr/colorForeground">
    <solid android:color="#1f000000" />
    <size
        android:height="1dp"
        android:width="1dp" />
</shape>
```

我们使用这个xml作为divider，效果如下

![](divider_item_decoration3.jpg)

# BorderItemDecoration
基于 DividerItemDecoration 的代码改造，实现一个边形式的 item decoration

![](border_item.jpg)

# 更多



http://stackoverflow.com/questions/24618829/how-to-add-dividers-and-spaces-between-items-in-recyclerview

https://github.com/gejiaheng/Dividers-For-RecyclerView

# 实际案例

魅族手机上缺省样式不一样

如何处理?




# ItemTouchHelper
ItemTouchHelper 实现也是 RecyclerView.ItemDecoration， 所以我们顺便也分析一下这个类。 它用于支持 swipe to dismiss 和 drag & drop 