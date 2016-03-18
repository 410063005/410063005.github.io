---
layout: post
title: "EditText中的输入表情问题"
date: 2016-03-16 15:56:02 +0800
comments: true
categories: android
published: false
---
本文讨论了如何在Android平台上实现表情输入。

<!--more-->

# EditText如何输入表情
系统自带输入法输入表情截图如下

![xiaomi]()

输入表情两个关键点如下：

1. SpannableString，`This is the class for text whose content is immutable but to which markup objects can be attached and detached`。SpannableString表示一个文本不可变的类，但其上可附着"markup objects"
2. ImageSpan。即上面说的"markup objects"。官方文档中并没有解释什么是ImageSpan，但网上可以找到不少的demo。

代码如下：

```
final EditText edit = emojiEdit;

Drawable drawable = ContextCompat.getDrawable(this, R.drawable.f_static_020);
// 不设置bounds时无法显示表情
drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

SpannableString spanned = new SpannableString("[smile]");
spanned.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE), 0, "[smile]".length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
edit.append(spanned);
```
简单吧。按照这个思路，你完全也可以实现类似QQ或微信那样的表情输入。

下面是一个稍完整的例子。 首先先定义表情类Emoji，Emoji由一个图标及一个关联的字符串构成：

```
class Emoji {
	final Drawable drawable; // 表情图标
	final CharSequence chars;// 表情字符

	public Emoji(Drawable drawable, CharSequence chars) {
		// 不设置bounds时无法显示表情
		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

		this.drawable = drawable;
		this.chars = chars;
	}
}
```

输入表情的代码如下，主要有三个步骤：

1. 点击代表表情的View后生成相应的Emoji
2. 将Emoji的字符串跟EditText中原来的字符串拼接生成新的字符串
3. EditText.setText设置新的字符串

```
@OnClick({R.id.emoji_demo, R.id.emoji_demo2})
public void onClick(View view) {
	final EditText edit = emojiEdit;

	Emoji emoji = getEmojiFromView(view);

	if (edit.getSelectionStart() != -1) {
		String text = edit.getText().toString();
		int start = 0;
		int end = text.length();
		int mid = edit.getSelectionStart(); // 记住插入表情的位置

		SpannableString spanned = new SpannableString(text.substring(start, mid) + emoji.chars + text.substring(mid, end));
		edit.setText(spanned);
		edit.setSelection(mid + emoji.chars.length()); // 插入的表情后光标移到表情字符之后
	} else {
		// TODO
	}
}

private Emoji getEmojiFromView(View view) {
	switch (view.getId()) {
		case R.id.emoji_demo:
			return new Emoji(ContextCompat.getDrawable(this, R.drawable.f_static_020), "\\:_");

		case R.id.emoji_demo2:
			return new Emoji(ContextCompat.getDrawable(this, R.drawable.f_static_000), "\\:)");
	}
	return new Emoji(ContextCompat.getDrawable(this, R.drawable.emoji_fallback), "\\:f");
}
```

要注意几个细节：

1. <font color="red">拼接顺序</font>。上述第2步中应注意拼接顺序，因为用户有可能是在中间位置输入表情
2. <font color="red">将字符串转换成ImageSpan</font>。简单起见，上述第3步只设置字符串，而并没有转换成ImageSpan。我们后面讨论如何将字符串转换成ImageSpan
3. <font color="red">便于用户连续输入</font>。为了较好的体验，setText之后应考虑将光标移到恰当地位置，便于用户连续输入

# 如何保证表情+文字顺序
上面的代码可以在EditText中输入表情，但如果你移动光标在已输入的表情中添加字符时(真实用户输入时会比较随意，可能会移动光标！)会发现字符有时无法正确显示。对比可以输入表情的输入法，如小米手机上自带的百度输入法，就没有类似问题。看来并没那么简单，我们漏掉了什么细节。

项目案例
校园项目中大量使用表情符号，允许用户评论时输入表情。所以也遇到上述问题。
存在的问题和Bug

用户反馈bug如下：`输入表情后，在前面再次插入字符就不会显示，但是发布出去之后显示了`

截图如下：

图片说明：先输入"啊啊啊啊吧[微笑]"，光标再移到[微笑]前输入字符，字符不能正确地显示出来。

## 问题1

## 问题2

如何解决的

如何避免afterTextChanged()的inifite loop

+ 增加`editText.getText().toString().compareTo(s.toString()) != 0`判断
+ 调用editText.setText()前`removeTextChangedListener`，调用后重新`addTextChangedListener`

微信是怎么解决的，存在什么问题 其他竞品

性能
习大大好不好

[how to write]: http://www.cnblogs.com/coffeedeveloper/p/4825177.html
[aftertextchanged
]: http://stackoverflow.com/questions/8628437/textwatcher-aftertextchanged-causes-stackoverflow-in-android
