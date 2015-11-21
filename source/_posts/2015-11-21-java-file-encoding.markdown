---
layout: post
title: "Java的Encoding"
date: 2015-11-21 19:34:19 +0800
comments: true
categories: java
published: true
---
Windows 7上使用[wire-compiler][wire-compiler]生成的Java文件，在Android Studio中打开时出现中文乱码。UTF-8时中文显示为乱码，切换为GBK时正常。而同事在Mac上使用同样的工具生成Java文件则不会遇到这个问题。推测是因为不同平台上生成的文件编码不同引起的，为什么会产生不同编码呢？
<!-- more -->

先讲下具体情况。`sample.proto`内容如下：

```
option java_package = "com.sample.proto";

// 大小
message Size
{
    optional    int32       width                   = 1;        // 宽
    optional    int32       height                  = 2;        // 高
}

// 正文
message Content
{
    optional    int32       type                    = 1;        // 类型 1文本 2图片 3视频
    optional    string      text                    = 2;        // 内容  
    optional    Size        size                    = 3;        // size
}
```

执行命令

```
java -jar wire-compiler-2.0.0-jar-with-dependencies.jar  --proto_path=. --java_out=tmp sample.proto
```

生成如下Java文件：

```
com\sample\proto\Content.java
com\sample\proto\Size.java
```

生成的文件无法用UTF8编码正常显示，且Android Studio编译这些文件时会出现警告。一开始认为是`wire-compiler.jar`这个工具的问题，毕竟Java文件是由它根据proto生成的。简单看了下[WireCompiler.java][WireCompiler]，只在注释中找到以下参数：

```
java WireCompiler --proto_path=<path> --java_out=<path>
      [--files=<protos.include>]
      [--includes=<message_name>[,<message_name>...]]
      [--excludes=<message_name>[,<message_name>...]]
      [--service_factory=<class_name>]
      [--service_factory_opt=<value>]
      [--service_factory_opt=<value>]...]
      [--quiet]
      [--dry_run]
      [--android]
      [--compact]
      [file [file...]]
```

并没提供参数以控制生成文件的编码。应该不是`wire-compiler`的问题。继承追，Java文件是由`writeJavaFile()`生成的，而这个方法又调用了`com.squareup.javapoet.JavaFile.writeTo()`方法。

```
import com.squareup.javapoet.JavaFile;

private void writeJavaFile(ClassName javaTypeName, TypeSpec typeSpec, Location location)
      throws IOException {
    JavaFile.Builder builder = ...
    JavaFile javaFile = builder.build();

    
    try {
		...
        javaFile.writeTo(path);
		...
    } catch (IOException e) {

    }
}
```

[javapoet][javapoet]又是什么呢？它的项目文档中这么介绍的。

> JavaPoet is a Java API for generating .java source files.

原来是Java源文件生成器。接着看刚才的`JavaFile.writeTo()`方法，它最后会调用到下面这个方法：

```
public void writeTo(Path directory) throws IOException {
    ...
    Path outputDirectory = directory;
    if (!packageName.isEmpty()) {
      for (String packageComponent : packageName.split("\\.")) {
        outputDirectory = outputDirectory.resolve(packageComponent);
      }
      Files.createDirectories(outputDirectory);
    }

    Path outputPath = outputDirectory.resolve(typeSpec.name + ".java");
    try (Writer writer = new OutputStreamWriter(Files.newOutputStream(outputPath))) {
      writeTo(writer);
    }
}
```

看到`OutputStreamWriter`这个熟面孔了！使用`OutputStreamWriter`要指定正确的的`Charset`才能避免乱码问题。贴两段代码就明白了：

```
public class OutputStreamWriter extends Writer {


    /**
     * Constructs a new OutputStreamWriter using {@code out} as the target
     * stream to write converted characters to. The default character encoding
     * is used.
     *
     */
    public OutputStreamWriter(OutputStream out) {
        this(out, Charset.defaultCharset());
    }
}
```

```
public abstract class Charset implements Comparable<Charset> {

    private static final Charset DEFAULT_CHARSET = getDefaultCharset();
	
    /**
     * Returns the system's default charset. This is determined during VM startup, and will not
     * change thereafter. On Android, the default charset is UTF-8.
     */
    public static Charset defaultCharset() {
        return DEFAULT_CHARSET;
    }

    private static Charset getDefaultCharset() {
        String encoding = System.getProperty("file.encoding", "UTF-8");
        try {
            return Charset.forName(encoding);
        } catch (UnsupportedCharsetException e) {
            return Charset.forName("UTF-8");
        }
    }
}
```

其实最终回到了一个基本的问题，Java的缺省编码方式是什么?[这篇博客][ref]中关于Java的`file.encoding`属性讲得非常清晰：

> This property is used for the default encoding in Java, all readers and writers would default to use this property. “file.encoding” is set to the default locale of Windows operationg system since Java 1.4.2. System.getProperty(“file.encoding”) can be used to access this property. Code such as System.setProperty(“file.encoding”, “UTF-8”) can be used to change this property. However, the default encoding can not be changed dynamically even this property can be changed. So the conclusion is that the default encoding can’t be changed after JVM starts. “java -Dfile.encoding=UTF-8” can be used to set the default encoding when starting a JVM. I have searched for this option Java official documentation. But I can’t find it.

翻译一下：Java的`file.encoding`属性用于指定缺省编码方式，所有的Reader和Writer都会使用这个属性指定的编码读写文件(注：当然也包括我们刚才看到的`OutputStreamWriter`)。从Java 1.4.2开始`file.encoding`自动设置为Windows操作系统的缺省locale。可以使用`System.getProperty("file.encoding")`方法访问这个属性。而`System.setProperty("file.encoding", "UTF-8")`会修改这个属性。但是，即使这个属性可以被修改，缺省的编码方式也不可动态修改。所以结论是JVM启动后缺省编码不会被修改。`java -Dfile.encoding=UTF-8`可用于启动JVM时指定缺省编码方式。

我在自己的机器上输出`System.getProperty("file.encoding", "UTF-8")`的返回值，果然是"GBK"。所以<font color="red">可以断定是我的Windows 7系统上会以"GBK"编码启动JVM，导致[wire-compiler][wire-compiler]输出相同编码的Java文件。而我的Android Studio中又以"UTF-8"打开Java文件，必然会出现中文乱码</font>。

![java_encoding](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/java_encoding.PNG)

再看看本文开头提到的问题，加上`-Dfile.encoding=UTF-8`参数后，即

`java -jar -Dfile.encoding=UTF-8 wire-compiler-2.0.0-jar-with-dependencies.jar  --proto_path=. --java_out=tmp sample.proto`

这次可以生成UTF8能正常显示的文件了！

[wire-compiler]: https://github.com/square/wire
[ref]: http://blog.sina.com.cn/s/blog_4ce8808d0101d0i1.html
[WireCompiler]: https://github.com/square/wire/blob/master/wire-compiler/src/main/java/com/squareup/wire/WireCompiler.java
[javapoet]: https://github.com/square/javapoet