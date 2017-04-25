---
title: jsonschema2pojo_usage
tags:
---


手写json的问题

在线版本 http://www.jsonschema2pojo.org/

插件  https://github.com/joelittlejohn/jsonschema2pojo/tree/master/jsonschema2pojo-gradle-plugin

语法 https://github.com/joelittlejohn/jsonschema2pojo/wiki/Reference

https://github.com/joelittlejohn/jsonschema2pojo

https://github.com/joelittlejohn/jsonschema2pojo/wiki/Reference#items

https://github.com/joelittlejohn/jsonschema2pojo/issues/678

# jsonschema2pojo
研究了下[jsonschema][jsonschema], 发现太复杂。我需要的不过是避免手写pojo代码，使用jsonschema有些杀鸡用牛刀的感觉。


[jsonschema]: http://json-schema.org/

# protobuf
之前项目使用protobuf协议。protobufc编译器通过pb源文件生成对应的Java/C++代码，非常方便。


[Protobuf 语法指南](http://colobu.com/2015/01/07/Protobuf-language-guide/)


这个[帖子](http://stackoverflow.com/questions/18261372/converting-a-protocol-buffer-to-a-pojo)提到了使用 ProtoStuff 中的java bean compiler

[protostuff](https://github.com/protostuff/protostuff)