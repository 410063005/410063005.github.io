---
layout: post
title: "开发经验记录"
date: 2015-06-15 11:59:17 +0800
comments: true
categories: tools
published: true
---
记录开发过程遇到的问题以及解决方法。备忘，以免再次遇到时到处乱翻。

<!--more-->

# Android相关
## X86模拟器
[BIOS中开启VT](http://blog.csdn.net/zklth/article/details/7921061)

[Android x86模拟器Intel Atom x86 System Image配置](http://www.eoeandroid.com/thread-192847-1-1.html)

![how_to_enable_vt](http://7xn5nf.com1.z0.glb.clouddn.com/image/blog/2015/11/how_to_enable_vt.png)

## Library项目
Eclipse下的Android项目跟Android Library项目区别在于`project.properties`中`android.library`属性取值。在使用Ant构建项目时，可以动态指定`android.library`属性

```
ant debug -Dandroid.library=true
```

# Shell相关
## 文本替换

```
sed -e "s/__/\n/g" sdk_log.txt
```

# 其他
## WAP定位
[jQuery JSONP 实践](http://www.cnblogs.com/cfanseal/archive/2009/05/19/1460382.html)

```
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
<meta http-equiv=Content-Type content="text/html;charset=utf-8">
<script src="http://libs.baidu.com/jquery/2.0.0/jquery.min.js"></script>

<script type="text/javascript">
    function myLocation() {
        $.ajax({
            url: "http://127.0.0.1:7778/",
         
            // the name of the callback parameter, as specified by the YQL service
            jsonp: "callback",
         
            // tell jQuery we're expecting JSONP
            dataType: "jsonp",
    
            // work with the response
            success: function( response ) {
                console.log( response ); // server response
                $("#raw").append("原始数据: " + response);
    
                $("#loc").append("定位结果: " + response['latitude'] + ',' + response['longitude'] + ',' + response['accuracy'] + ',' + response['address']);
            }
        });
    }
    
    myLocation();
    
    $(document).ready(function(){
          $("#clean").click(function(){
            	$("#raw").empty();
              	$("#loc").empty();
          });
        
        $("#locate").click(function() {
          	$("#raw").empty();
           	$("#loc").empty();
            
        	myLocation();
        });
        
    });
</script>
</head>
```

## Eclipse技巧
删除行尾空格 "Remove Trailing Whitespace"

## 坐标及坐标转换
[地图经纬度及坐标系统转换的那点事](http://www.biaodianfu.com/coordinate-system.html)

[国内各地图API坐标系统比较](http://www.cnblogs.com/Tangf/archive/2012/03/15/2398397.html)

