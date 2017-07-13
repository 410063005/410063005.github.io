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




# 参考资料
[相关视频](https://www.youtube.com/watch?v=sO9aX87hq9c)

[Guideline的用法][evenly-spacing-views-using-constraintlayout]

[ref]: http://km.oa.com/group/29501/articles/show/307370?kmref=search&from_page=1&no=2
[ref2]: http://km.oa.com/group/21869/articles/show/280008?kmref=search&from_page=1&no=4
[evenly-spacing-views-using-constraintlayout]: https://stackoverflow.com/questions/37518745/evenly-spacing-views-using-constraintlayout
[wiki]: https://android-developers.googleblog.com/2016/05/android-studio-22-preview-new-ui.html