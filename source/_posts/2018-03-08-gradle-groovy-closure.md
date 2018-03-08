---
title: 理解Groovy闭包委托
date: 2018-03-08 17:14:21
tags: gradle
---
[TODO]
<!--more-->
[Closure](https://github.com/groovy/groovy-core/blob/master/src/main/groovy/lang/Closure.java#L97)代码

```groovy

```

# thisObject, owner, delegate
Closure有三个属性，分别是 thisObject, owner, delegate。通常delegate被设置为owner。  [来源](https://stackoverflow.com/questions/8120949/what-does-delegate-mean-in-groovy)

Idea的Groovy Console中运行代码：

```groovy
def testClosure(closure) {
    closure()
}
testClosure() {
    println "this is " + thisObject + ", super:" + this.getClass().superclass.name
    println "owner is " + owner + ", super:" + owner.getClass().superclass.name
    println "delegate is " + delegate + ", super:" + delegate.getClass().superclass.name

    testClosure() {
        println "this is " + thisObject + ", super:" + this.getClass().superclass.name
        println "owner is " + owner + ", super:" + owner.getClass().superclass.name
        println "delegate is " + delegate + ", super:" + delegate.getClass().superclass.name
    }
}
```

输出如下：

```
this is ideaGroovyConsole@710636b0, super:groovy.lang.Script
owner is ideaGroovyConsole@710636b0, super:groovy.lang.Script
delegate is ideaGroovyConsole@710636b0, super:groovy.lang.Script
this is ideaGroovyConsole@710636b0, super:groovy.lang.Script
owner is ideaGroovyConsole$_run_closure1@4e5ed836, super:groovy.lang.Closure
delegate is ideaGroovyConsole$_run_closure1@4e5ed836, super:groovy.lang.Closure
```

# resolveStrategy

# demo