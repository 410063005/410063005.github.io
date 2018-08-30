---
title: 使用Kotlin Android Extensions时碰到的一个问题
date: 2018-04-28 16:30:27
tags: Android
toc: true
---
本文记录了使用kotlin android extions时遇到的小问题以及对应的解决方法。
<!--more-->
# Kotlin Android Extensions
[Kotlin Android Extensions][ref]是用于Kotlin android开发的插件。

```
apply plugin: 'kotlin-android-extensions'
```

借助该插件我们在Kotlin代码中不必再使用`findViewById()`，直接通过view id访问相应View即可。例如：

```kotlin
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main_clip.*

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

	btn_clip_demo.setOnClickListener {}
	iv_demo.setImageDrawable(ContextCompat.getDrawable(this, R.color.colorAccent))
}
```

对Java代码中繁琐的`findViewById()`方便许多，对吧。

更多用法可以参考[这篇文章][kotlin-android-extension]。

# 遇到的问题
很不幸，使用[Kotlin Android Extensions][ref]时遇到一个奇怪的问题。布局和代码如下：

activity_main.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context="com.example.cm.drawabledemo.MainActivity">

    <FrameLayout
        android:id="@+id/fl_container"
        android:layout_width="0dp"
        android:layout_height="300dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintLeft_toLeftOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <Button
        android:id="@+id/btn_clip_demo"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="8dp"
        android:layout_marginTop="8dp"
        android:text="clip demo"
        app:layout_constraintStart_toEndOf="@+id/btn_gravity"
        app:layout_constraintTop_toTopOf="parent" />
</android.support.constraint.ConstraintLayout>
```

content_main_clip.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    <ImageView
        android:padding="2dp"
        android:id="@+id/iv_demo"
        android:layout_width="92dp"
        android:layout_height="78dp"
        android:layout_marginEnd="24dp"
        android:layout_marginTop="104dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
</android.support.constraint.ConstraintLayout>
```

MainActivity.kt

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
		
        btn_clip_demo.setOnClickListener {
            fl_container.removeAllViews()
            layoutInflater.inflate(R.layout.content_main_clip, fl_container)
			iv_demo.setImageDrawable(ContextCompat.getDrawable(this, R.color.colorAccent))
		}
	}
}
```

代码功能：点击`btn_clip_demo`后，将`content_main_clip`中的内容(其中有一个id为`iv_demo`的ImageView)重新添加到`fl_container`，并且将`iv_demo`设置为红色。

问题描述：第一次点击`btn_clip_demo`，`iv_demo`被设置成红色。<font color="red">但之后再点击btn_clip_demo，iv_demo没有被设置成红色</font>。

![](koltin-android-ext1.gif)

# 问题分析

Android Studio查看Kotlin对应Java代码的方式： Tools -> Kotlin -> Show Kotlin Bytecode -> Decompile

上述kotlin代码对应的Java代码如下：

```java
public final class MainActivity extends AppCompatActivity {
   private HashMap _$_findViewCache;

   protected void onCreate(@Nullable Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.setContentView(2131296283);
      ((Button)this._$_findCachedViewById(id.btn_clip_demo)).setOnClickListener((OnClickListener)(new OnClickListener() {
         public final void onClick(View it) {
            ((FrameLayout)M.this._$_findCachedViewById(id.fl_container)).removeAllViews();
            M.this.getLayoutInflater().inflate(2131296285, (FrameLayout)M.this._$_findCachedViewById(id.fl_container));
            ((ImageView)M.this._$_findCachedViewById(id.iv_demo)).setImageDrawable(ContextCompat.getDrawable((Context)M.this, 2130968614));
         }
      }));
   }

   public View _$_findCachedViewById(int var1) {
      if(this._$_findViewCache == null) {
         this._$_findViewCache = new HashMap();
      }

      View var2 = (View)this._$_findViewCache.get(Integer.valueOf(var1));
      if(var2 == null) {
         var2 = this.findViewById(var1);
         this._$_findViewCache.put(Integer.valueOf(var1), var2);
      }

      return var2;
   }

   public void _$_clearFindViewByIdCache() {
      if(this._$_findViewCache != null) {
         this._$_findViewCache.clear();
      }

   }
}
```

高大上的Kotlin Android Extensions生成的代码其实非常简单，要点如下：

+ View cache策略
 + 自动生成一个HashMap类型的findViewCache，用于缓存访问过的View
 + 自动生成findCachedViewById()和clearFindViewByIdCache()方法
+ Kotlin代码中对view id的直接访问被转换成相应的findCachedViewById()调用

```kotlin
btn_clip_demo.setOnClickListener {
    fl_container.removeAllViews()
    layoutInflater.inflate(R.layout.content_main_clip, fl_container)
    iv_demo.setImageDrawable(ContextCompat.getDrawable(this, R.color.colorAccent))
}
```

对照代码不难理解问题的原因在于：

第一次点击`btn_clip_demo`时，view cache策略并不生效，所以一切正常。第二次点击`btn_clip_demo`时，view cache策略生效。但view cache策略对我们的场景是错误的(从另一角度讲，可能是我的用法有误？)。从view cache中拿到的`iv_demo`对象是旧的。关键是，它已经从屏幕上移除，是不可见的。我们给它设置红色当然不起作用！而`inflate()`操作新添加到布局中的、在屏幕上可见的那个`iv_demo`，实际上被我们晾在一边。

所以这里我们不得不使用`findViewById`来获取那个被晾在一边的新的`iv_demo`。以下是修改后的代码：

```kotlin
btn_clip_demo.setOnClickListener {
    fl_container.removeAllViews()
    layoutInflater.inflate(R.layout.content_main_clip, fl_container)
	val ivDemoNew = findViewById<ImageView>(R.id.iv_demo)
    ivDemoNew.setImageDrawable(ContextCompat.getDrawable(this, R.color.colorAccent))
}
```

![](koltin-android-ext2.gif)

这回，无论我们如何点击按钮，`iv_demo`永远会被正确地设置为红色。

[kotlin-android-extension]: https://antonioleiva.com/kotlin-android-extensions/
[ref]: https://kotlinlang.org/docs/tutorials/android-plugin.html

