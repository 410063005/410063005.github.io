---
layout: post
title: "JSONException分析"
date: 2015-05-27 17:22:07 +0800
comments: true
categories: android
---
本文记录如何分析定位SDK出现的JSONException。

<!--more-->

定位SDK中出现`org.json.JSONException: End of input at character 0 of`这种异常。完整信息如下：

```
06-19 13:22:32.229: W/System.err(19647): org.json.JSONException: End of input at character 0 of 
06-19 13:22:32.264: W/System.err(19647):    at org.json.JSONTokener.syntaxError(JSONTokener.java:450)
06-19 13:22:32.265: W/System.err(19647):    at org.json.JSONTokener.nextValue(JSONTokener.java:97)
06-19 13:22:32.268: W/System.err(19647):    at org.json.JSONObject.<init>(JSONObject.java:154)
06-19 13:22:32.269: W/System.err(19647):    at org.json.JSONObject.<init>(JSONObject.java:171)
```

非常不解。看字面意思是第0个字符处结束了，推测 `character 0 of`后面其实有个空白字符(不可见)。

比较赞成[这个解释](http://stackoverflow.com/questions/24301521/jsonexception-end-of-input-at-character-0)：

> You are probably getting a blank response. Its not null but the jsontext is empty. So you are getting this error and not a Nullpointer exception

跑下面这个单元测试，果然复现问题

```
	public void testJson() throws Exception {
		new JSONObject("");
	}
```

做点小修改，让这个不可见的空白字符显身。修改后的代码如下：

```
	public void testJson() throws Exception {
		new MyJSONObject("");
	}
	
	static class MyJSONObject extends JSONObject {
		public MyJSONObject(String json) throws JSONException {
			super(new MyJSONTokener(json));
		}
	}
	
	static class MyJSONTokener extends JSONTokener {
		private String mIn;

		public MyJSONTokener(String in) {
			super(in);
			mIn = in;
		}
		
		@Override
		public String toString() {
			String res = super.toString();
			if ("".equals(mIn)) {
				res += "'" + mIn + "'";
			}

			return res;
		}
	}

```

此时的异常信息更加明确了，`org.json.JSONException: End of input at character 0 of ''`。

至此疑惑全解，出错的原因就是传入`JSONObject()`构造方法的字符串为空，空字符串是非法的JSON格式，解析出错很正常。所以问题真正的原因就要从为什么会出现空字符串查找了，这是另一个话题。
