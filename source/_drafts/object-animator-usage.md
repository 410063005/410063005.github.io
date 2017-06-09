---
title: ObjectAnimator简介
tags:
 - android
---
介绍了`ObjectAnimator`的使用方法

<!-- more -->

动画效果如下:

![动画效果](fire.gif)

代码如下：

```java
    private void animateFire() {
        float x1 = tvTodayCheck.getWidth() / 2;
        float x2 = tvTodayCheck.getWidth() + 5;
        ObjectAnimator c = ObjectAnimator.ofFloat(llFire, "translationX", 0, x1);
        ObjectAnimator d = ObjectAnimator.ofFloat(tvTodayCheck, "translationX", 0, x2);

        ObjectAnimator e = ObjectAnimator.ofFloat(ivFire, "scaleX", 1f, 1.5f, 1f);
        ObjectAnimator f = ObjectAnimator.ofFloat(ivFire, "scaleY", 1f, 1.5f, 1f);

        // 火苗和点火按钮横向移动
        AnimatorSet s1 = new AnimatorSet();
        s1.playTogether(c, d);
        s1.setDuration(400);

        // 火苗放大缩小
        AnimatorSet s2 = new AnimatorSet();
        s2.playTogether(e, f);
        s2.setDuration(150);

        AnimatorSet s3 = new AnimatorSet();
        s3.playSequentially(s1, s2);
        s3.start();
    }
    
    tvTodayCheck.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            animateFire();
        }
    });
```

主要用到了`ObjectAnimator`和`AnimatorSet`。

# ObjectAnimator
ObjectAnimator是ValueAnimator的子类，支持在目标对象上使用属性动画。这个类的构造方法接收目标对象，以及将要显示动画的属性名作为参数。ObjectAnimator内部会调用属性相应的set/get方法以显示动画。

也可以从资源文件创建ObjectAnimator：

```xml
<objectAnimator xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="1000"
    android:valueTo="200"
    android:valueType="floatType"
    android:propertyName="y"
    android:repeatCount="1"
    android:repeatMode="reverse"/>
```

通过`AnimatorInflater.loadAnimator()`方法从资源文件中加载属性动画，用法如下：

```java
ObjectAnimator an = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.scale_down);
an.setTarget(findViewById(R.id.iv));
an.start();
```

# AnimatorSet



[ref]: https://developer.android.com/reference/android/animation/ObjectAnimator.html
[ref2]: https://developer.android.com/guide/topics/graphics/prop-animation.html#object-animator
