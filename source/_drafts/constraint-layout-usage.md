---
title: ConstraintLayout用法简介
tags:
 - android
---
本文介绍了`ConstraintLayout`布局的用法。
<!--more-->

从公司内部论坛上看到一篇[介绍ContraintLayout的文章][ref]，文中提到了它的诸多优点，比如，能够扁平化地实现以前很多用LinearLayout或RelativeLayout实现起来很麻烦的布局。ContraintLayout作为Android中新引入的Layout，有必要了解并掌握其用法。

# 介绍
ConstraintLayout是2016年Google IO大会中随[Android Studio 2.2预览版][wiki]发布的。可以理解为RelativeLayout的升级版本，强调约束关系。可以让布局更加扁平化，适合于不需要滚动的布局。


目前最新版本是1.0.2。使用ConstaintLayout时首先要添加依赖，

```
compile 'com.android.support.constraint:constraint-layout:1.0.2'
```

版本号中已去掉了`beta`字段。

# 用法
## Layout Editor的使用
[这篇文章][wiki2]介绍了Layout Editor的使用。

## Chain

## Guideline
什么是Guideline

# 官方
[官方介绍](https://developer.android.com/reference/android/support/constraint/ConstraintLayout.html)

`ConstraintLayout`布局是一个ViewGroup，它允许以非常灵活的方式给widget设置位置和大小。

## 相对定位
相对定位。相对定位是在ConstraintLayout中创建布局的最基本方式。这种约束允许你相对于指定的widget来进行定位。可以在水平方向或垂直方向上对widget约束：

+ 水平方向：约束条件包括左边，右边，起始边，结束边
+ 垂直方向：约束条件包括上边，下边以及文本基线

最基本的要点就是指定别的widget的某个边来对widget的指定边进行约束。

比如，要指定B按钮在A按钮的右边

![](https://developer.android.com/reference/android/support/constraint/resources/images/relative-positioning.png)

代码如下：

```xml
<Button android:id="@+id/buttonA" ... />
         <Button android:id="@+id/buttonB" ...
                 app:layout_constraintLeft_toRightOf="@+id/buttonA" />
```

这段代码的意思是想让B按钮的左边由A按钮的右边来约束。这个位置约束意味着系统将会让这两个边位于同一个位置。

![](https://developer.android.com/reference/android/support/constraint/resources/images/relative-positioning-constraints.png)

其他的约束条件还包括：

+ layout_constraintLeft_toLeftOf
+ layout_constraintLeft_toRightOf
+ layout_constraintRight_toLeftOf
+ layout_constraintRight_toRightOf
+ layout_constraintTop_toTopOf
+ layout_constraintTop_toBottomOf
+ layout_constraintBottom_toTopOf
+ layout_constraintBottom_toBottomOf
+ layout_constraintBaseline_toBaselineOf
+ layout_constraintStart_toEndOf
+ layout_constraintStart_toStartOf
+ layout_constraintEnd_toStartOf
+ layout_constraintEnd_toEndOf

所有这些约束条件会引用另一个widget的id，或者`parent`

## Margin约束
![](https://developer.android.com/reference/android/support/constraint/resources/images/relative-positioning-margin.png)

如果设置了margin，它们会应用于相应的约束，以作为两边之间的空白。通常的margin包括：

+ android:layout_marginStart
+ android:layout_marginEnd
+ android:layout_marginLeft
+ android:layout_marginTop
+ android:layout_marginRight
+ android:layout_marginBottom

注意margin只能为正数或0，并且带单位。

当位置约束的目标widget的可见性为`View.GONE`时，同样可以使用以下属性指定margin：

+ layout_goneMarginStart
+ layout_goneMarginEnd
+ layout_goneMarginLeft
+ layout_goneMarginTop
+ layout_goneMarginRight
+ layout_goneMarginBottom

## 居中定位
`ConstraintLayout`的一个非常有用的方面是它如何处理那些貌似不可能的约束。比如，如果我们有以下代码：

```xml
<android.support.constraint.ConstraintLayout ...>
             <Button android:id="@+id/button" ...
                 app:layout_constraintLeft_toLeftOf="parent"
                 app:layout_constraintRight_toRightOf="parent/>
         </>
```

除非`ConstraintLayout`恰好跟里面的Button一样大，否则左右两边的约束不可能同时满足。

![](https://developer.android.com/reference/android/support/constraint/resources/images/centering-positioning.png)

这种情况下两边的约束可以想象成水平方向上两个方向相反、对widget进行拉伸的作用力，由于作用力大小相同，所以widget最终居中。垂直方向情况类似。

缺省时上述情形中相反约束会让widget居中，但可以使用bias属性对位置进行微调：

+ layout_constraintHorizontal_bias
+ layout_constraintVertical_bias

![](https://developer.android.com/reference/android/support/constraint/resources/images/centering-positioning-bias.png)

比如以下代码左边有30%的bias，而不是缺省的50%，所以左边会短一些，widget也更靠近左边。

```xml
<android.support.constraint.ConstraintLayout ...>
             <Button android:id="@+id/button" ...
                 app:layout_constraintHorizontal_bias="0.3"
                 app:layout_constraintLeft_toLeftOf="parent"
                 app:layout_constraintRight_toRightOf="parent/>
         </>
```
使用bias可以更好地适配屏幕尺寸的变换。

## 可见性
`ConstraintLayout`对标记为`View.GONE`的widget有特别的处理。

`GONE`widget仍然不可见，也不属性布局的一部分(比如，它们的实际宽高不会被改变)。

但从布局计算角度，`GONE`widget仍然是布局的一部分，但有以下区别：

+ 在布局阶段，这些widget的宽高被认为是0
+ 如果这些widget对其他widget有约束，这些约束仍然生效，但所有的margin都认为是0

![](https://developer.android.com/reference/android/support/constraint/resources/images/visibility-behavior.png)

这里的特殊行为可以构建临时将widget设置为`GONE`的布局，这对实现简单的布局动画非常有用。

**注意:** 上图中A隐藏后将使用B中定义的margin。而有时，这可能不是你想要的结果。比如，A相对它的parent有100dp的margin，而B有相对于A的16dp的margin，当隐藏A之后，B相对于parent的margin是16dp。基于此，B相对于A、但A被设置为`GONE`这一情形指定一个特别的margin。

## 宽高约束
### ConstraintLayout最小宽高
可以为`ConstraintLayout`自身指定最小尺寸

+ `android:minWidth`指定布局的最小宽度
+ `android:minHeight`指定布局的最小高度

当ConstraintLayout的宽高被设置为`WRAP_CONTENT`时上述宽高将被使用。

### Widget宽高约束

+ 使用固定的尺寸(比如123dp这样的字面量)
+ 使用`WRAP_CONTENT`，让widget自行计算尺寸
+ 使用`0dp`，等价于`MATCH_CONSTRAINT`

![](https://developer.android.com/reference/android/support/constraint/resources/images/dimension-match-constraints.png)

前两者跟其他布局类似。而后者将根据设置的约束来调整widget的大小。见上图，(a)是wrap_content (b)是0dp。如果设置了margin，则margin也参与计算。见(c)

**重要:** `ConstraintLayout`中的widget不支持`match_parent`

### 比例
也可以使用定义widget某一边相对于另一边的比例来规定尺寸。要使用比例，必须至少将一个尺寸约束设置为0dp，并设置属性`layout_constraintDimentionRatio`为指定的值。例如：

```xml
<Button android:layout_width="wrap_content"
                   android:layout_height="0dp"
                   app:layout_constraintDimensionRatio="1:1" />
```

以上代码将按钮的高度和宽度设置为相同。

比例可以使用两种方式表示：

+ 一个浮点数，表示宽度和高度之间的比例
+ 一个"width:height"形式的比例

当宽高都为0dp时也可以使用比例。这种情况下系统会让较大的边满足所有的约束，并保证指定的比例。想以另一个边约束指定的边，可以添加`W`或`H`前缀，它们分别用来限制宽度和高度。比如，如果一个尺寸被两个条件约束(宽度为0dp，且居中)，可以使用前缀来指定哪个边需要被限制。

```xml
<Button android:layout_width="0dp"
                   android:layout_height="0dp"
                   app:layout_constraintDimensionRatio="H,16:9"
                   app:layout_constraintBottom_toBottomOf="parent"
                   app:layout_constraintTop_toTopOf="parent"/>
```

以上代码会按16:9的比例设置高度，并且让宽度满足约束条件。

## Chain
链接在单个方向上(水平方向或垂直方向)提供分组的行为。另一个方向上可以单独地约束。

### 创建链
通过双向约束连接在一起的一组widget称为链。下图是一个最小的链，只有两个widget

![](https://developer.android.com/reference/android/support/constraint/resources/images/chains.png)

### 链头
一组链中的第一个元素控制这条链。

![](https://developer.android.com/reference/android/support/constraint/resources/images/chains-head.png)

水平链中最左边的widget是链头，而垂直链中的最上边的widget是链头。

### 链中的margin

If margins are specified on connections, they will be taken in account. In the case of spread chains, margins will be deducted from the allocated space.

### 链的样式
通过在链头中设置`layout_constraintHorizontal_chainStyle `或`layout_constraintVertical_chainStyle`来控制链的行为。链的行为会依据指定的风格而改变(缺省的风格是`CHAIN_SPREAD`)。

+ `CHAIN_SPREAD` - 所有元素都会扩展
+ 带权重的链 - `CHAIN_SPREAD`模式中，如果某些widget设置为0dp，它们会平分剩下的空间
+ `CHAIN_SPREAD_INSIDE` - 与上面的类似，但链两端的元素不会扩展
+ `CHAIN_PACKED` - 链中的元素会排列到一起。水平方向或垂直方向的bias属性会影响packed元素的位置

![](https://developer.android.com/reference/android/support/constraint/resources/images/chains-styles.png)

权重链中各个元素以相同比例占据可用空间。如果一个或多个元素为0dp，它们会使用剩余的空间(平均分配)。`layout_constraintHorizontal_weight`或`layout_constraintVertical_weight`用于控制那些0dp的元素如何分配空间。比如，如果一个链中有两个元素，都是0dp，第一个元素权重为2，第二个元素权重为1，则前者占据的空间是后者的两倍。

## 视觉辅助对象
除了上述内容外，还可以在`ConstraintLayout`中使用特殊的辅助元素来帮助定位。可以创建水平或垂直的guideline，它们的位置约束相对于parent。接着可以使用这些guideline来定位widget。

[guideline][guideline]不会显示出来(它们的可见性是`View.GONE`)，仅仅用于布局，且只在`ConstraintLayout`中起作用。

Guideline可以是水平或垂直的：

+ 垂直guideline宽度为0且高度跟parent一致
+ 水平guideline高度为0且宽度跟parent一致

使用guideline定位有三种不同的方式：

+ 指定跟左边或上边的距离(`layout_constraintGuide_begin`)
+ 指定跟右边或下边的距离(`layout_constraintGuide_end`)
+ 指定占宽度或高度的百分比(`layout_constraintGuide_percent`)

widget可以由guideline来约束，多个widget可以由同一个guideline约束。而百分比定位则可实现响应式布局。

下面代码演示了如休使用垂直guideline对一个button进行约束：

```xml
<android.support.constraint.ConstraintLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        android:layout_width="match_parent"
        android:layout_height="match_parent">

    <android.support.constraint.Guideline
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:id="@+id/guideline"
            app:layout_constraintGuide_begin="100dp"
            android:orientation="vertical"/>

    <Button
            android:text="Button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:id="@+id/button"
            app:layout_constraintLeft_toLeftOf="@+id/guideline"
            android:layout_marginTop="16dp"
            app:layout_constraintTop_toTopOf="parent" />

</android.support.constraint.ConstraintLayout>
```

# 总结
好用的地方

左右拉力相同时居中

链 - 同时控制一组元素的布局

guideline - 根据百分比精确定位，响应式布局

bias - 响应式布局

ratio属性 - 图片自适应

## 案例
例1
什么时候使用比较合适?

![视觉图1](demo1.png)

第一个区域适合使用ConstraintLayout实现

例2
banner位图片自适应问题

例3
动画效果 见[官方demo][官方demo]

# 参考资料
[相关视频](https://www.youtube.com/watch?v=sO9aX87hq9c)
[Guideline的用法][evenly-spacing-views-using-constraintlayout]
[官方demo](https://github.com/googlesamples/android-ConstraintLayoutExamples)

[guideline]: https://developer.android.com/reference/android/support/constraint/Guideline.html
[ref]: http://km.oa.com/group/29501/articles/show/307370?kmref=search&from_page=1&no=2
[ref2]: http://km.oa.com/group/21869/articles/show/280008?kmref=search&from_page=1&no=4
[evenly-spacing-views-using-constraintlayout]: https://stackoverflow.com/questions/37518745/evenly-spacing-views-using-constraintlayout
[wiki]: https://android-developers.googleblog.com/2016/05/android-studio-22-preview-new-ui.html