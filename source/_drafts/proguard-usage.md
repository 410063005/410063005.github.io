---
title: 关于Proguard
tags:
 - android
---
本文介绍了Proguard的用法，常用库的Proguard配置，以及可能遇到的问题。

<!-- more -->

# Proguard的用法

# 常用库的配置

## Gson
Gson的proguard配置见[这里][gson]。

Gson使用类文件中保存的泛型信息来处理类的字段。而缺省情况下Proguard会移除这些信息，所以需要修改缺省配置以保留它们。

```java
public static int getNotifyFlag(Context context, String conversationId){
    HashMap<String, Integer> map = getPrefUserNotifyFlag(context);
    if(map == null || !map.containsKey(conversationId)){
        return -1;
    }
    try {
        return map.get(conversationId);
    } catch (ClassCastException e) {
        Logger.e(e);
        return -1;
    }
}

private static HashMap<String, Integer> getPrefUserNotifyFlag(Context context){
    String wrapperStr = getUserSharedPreferences(context).getString(PREF_USER_NOTIFY_FLAG, "");
    Gson gson = new Gson();
    IntMapWrapper wrapper = gson.fromJson(wrapperStr, IntMapWrapper.class);
    if(wrapper == null){
        return null;
    }
    return wrapper.getMyMap();
}
```

以上代码非常奇怪，`map.get(conversationId)`处老是报`ClassCastException`，提示将Float转换成Integer。

// TODO IntMapWrapper的代码是怎样的

而map是`HashMap<String, Integer>`类型的，怎么可能将Float值存到其中呢？经排查是proguard配置问题。

[Gson的proguard配置规则][gson]中明确提到，通过gson序列化的类不能被proguard处理。所以需要如下配置：

```
# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }
```

保留`IntMapWrapper`类后，上述`ClassCastException`问题解决。

# 可能遇到的问题

[gson]: https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg
