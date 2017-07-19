---
title: 源码学习之ColorPicker
tags:
 - android
---
最近对图象相关的东西比较感兴趣，于是找到了[ColorPicker][source]这个项目。随便看了下，发现项目虽小点赞数也不多，但还是挺有意思的。其中有不少知识点运用，比如自定义View、触摸事件的监听、矢量图、drawable、Shader、RGB与HSV的转换、Maven库上传。所以在此记录下。
<!-- more -->

# 总体结构
项目结构比较简单

![](arch.jpg)

+ `MainActivity`演示了`ColorPickerDialog`的用法
+ `ColorPickerDialog`是整个项目的核心
+ `ColorPlateView`实现了调色板功能，是比较有意思的地方
+ `OnColorPickerListener`是用于选定颜色或取消选定的回调

ColorPicker的效果如下

![](color-picker.gif)

# 知识点

+ 自定义View
+ 触摸事件监听
+ 矢量图
+ drawable的tileMode属性, 渐变色的使用
+ Shader

## 自定义View
`ColorPlateView`是自定义View。这个类代码并不多，关键点在于`onDraw()`中使用设置了Shader的Paint绘制带有混合渐变效果的矩形。

## 触摸事件监听
`ColorPickerDialog`中有三处监听触摸事件，分别是色彩板的触摸监听、透明度的触摸监听、调色板的触摸监听。监听处理的过程都比较类似：

1. 先根据y值计算新的颜色分量值
2. 再根据新的颜色分量修改相关UI
3. 根据y值移动光标位置
4. 回调，通知颜色变化

## 矢量图
android 5.0(API level 21)开始支持矢量图。通过support library，矢量图可以兼容到API level 7，矢量图动画可以兼容到API level 11。关于矢量图，见我的[另一篇文章](../vector-drawable)。

## drawable
drawable资源文件中支持`android:tileMode`中属性，ColorPicker中很好地演示了该属性的用法。实际的视觉资源其实是10x10px的小图片(仅135字节)，使用`repeat`的tileMode将其扩展为我们需要的效果。

```xml
<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/alpha_bottom"
    android:tileMode="repeat">
</bitmap>
```

![实际视觉资源](alpha_bottom.png)

![透明度选择区域](tile_mode.jpg)

除了可以在drawable xml文件中使用`<gradient>`，也可以直接使用`GradientDrawable`来创建渐变色。

## Shader
`Shader`类有以下几个子类，

![](shader.jpg)

+ `ComposeShader`可以组合另外的两个`Shader`
+ `LinearGradient`, `RadialGradient`, `SweepGraident`分别实现了不同的颜色渐变效果

这段代码演示了Shader的用法：

```java
protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (mPaint == null){
        mPaint = new Paint();
        mShaderVertical = new LinearGradient(0.f,0.f,0.f,this.getMeasuredHeight(),0xffffffff,0xff000000, Shader.TileMode.CLAMP);//线性渐变
    }
    int rgb = Color.HSVToColor(HSV);
    LinearGradient shaderHorizontal = new LinearGradient(0.f,0.f,this.getMeasuredWidth(),0.f,0xffffffff,rgb,Shader.TileMode.CLAMP);
    ComposeShader composeShader = new ComposeShader(mShaderVertical,shaderHorizontal,PorterDuff.Mode.MULTIPLY );//混合渐变
    mPaint.setShader(composeShader);
    canvas.drawRect(0.f,0.f,this.getMeasuredWidth(),this.getMeasuredHeight(),mPaint);
}
```
[HSV颜色模型](http://baike.baidu.com/link?url=oIluZmF9w5H6X861QaS_utuLeBr87gr4pRdbqHQZQgtXNocK2vONBEdsoSFvJZPBryclLkTtreRYvqRFOkFdAK)不同于ARGB。[由RGB到HSV颜色空间的理解](http://blog.csdn.net/viewcode/article/details/8203728)介绍了一些关于HSV颜色模型的知识。

在Android中，`void Color.colorToHSV(int color, float[] hsv)`方法将ARGB颜色转换成HSV，`int Color.HSVToColor(float[] hsv)`方法将HSV颜色转换成ARGB。

# 改进思路
+ SeekBar避免自己实现触摸事件监听 
+ `ColorPlateView`设计问题
+ `ColorPickerDialog`继承`DialogFragment`, 增加易用性

经实践，发现使用SeekBar避免自己实现触摸事件监听是非常麻烦的。原因有两点：

1. Android不直接支持垂直SeekBar，需要实现[VerticalSeekBar](https://github.com/jeisfeld/Augendiagnose/blob/master/AugendiagnoseIdea/augendiagnoseLib/src/main/java/de/jeisfeld/augendiagnoselib/components/VerticalSeekBar.java)
2. SeekBar的Thumb默认是对称、居中的，而ColorPicker中Thumb是在垂直方向偏左的。这一块需要重新调整，或者专门切图

`ColorPlateView`设计其实是有问题的。更好方式是借鉴Android的widget的实现，在自己内部覆盖`onTouchEvent()`方法。这样可以避免相关逻辑跟`ColorPickerDialog`耦合。

[source]: https://github.com/DingMouRen/ColorPicker
[blog]: http://group.jobbole.com/30613
[VectorDrawable怎么玩]: http://www.jianshu.com/p/456df1434739