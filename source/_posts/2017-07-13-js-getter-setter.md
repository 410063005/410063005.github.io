---
title: Javascript中的getter和setter
tags:
  - javascript
date: 2017-07-13 14:13:33
---

本文是《Eloquent Javascript》 "The Secret Lift of Objects"这一章节的学习笔记，记录了Javascript中对象的getter/setter的用法。

<!--more-->

类似于Java，Javascript中也提倡使用getter/setter而不是直接访问对象属性。但getter/setter的问题是你不得额外写大量的方法。

Javascript提供了一种兼有属性和getter/setter优势的技巧。可以给对象定义一个属性，从外部看来是一个正常的属性，但实际上是方法。

```javascript
var pile = {
    elements: ["eggshell", "orange peel", "worm"],
    get height() {
        return this.elements.length;
    },
    set height(value) {
        console.log("Ignoring attempt to set height to", value);
    }
};
console.log(pile.height);
// → 3
pile.height = 100;
// → Ignoring attempt to set height to 100
```

给属性添加`get`和`set`注解允许你定义相应的方法，这些方法会在访问属性时被调用。

```javascript
function TextCell(text) {
    this.text = text.split("\n");
}

Object.defineProperty(TextCell.prototype , "heightProp", {
    get: function() { return this.text.length; }
});

var cell = new TextCell("no\nway");
console.log(cell.heightProp);
// → 2
cell.heightProp = 100;
console.log(cell.heightProp);
// → 2
```

可以使用类似的方式定义`set`属性来指定setter方法。当有getter而没有setter时，向属性写入值时会被忽略。