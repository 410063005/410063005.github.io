---
layout: post
title: python中的strftime格式化参数
keywords: python
description: python小技巧
categories: [python,备忘]
tags: [python]
group: archive
icon: globe
---
>来自 python 文档 strftime() and strptime() Behavior

获取当前时间，并通过字符串输出。
格式为：`%Y-%m-%d %H:%M:%S`

	datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S' )

获取当前时间，但只保留日期

	datetime.datetime.now().date()
	
将字符串转换为datetime类型
输入字符串格式为：`%Y-%m-%d`

	datetime.datetime.strptime(time,'%Y-%m-%d')

附录：
格式化符号汇总

参数   |  含义
----- |------
%a    | 星期几的简写 Weekday name, abbr.
%A    | 星期几的全称 Weekday name, full
%b    | 月分的简写 Month name, abbr.
%B    | 月份的全称 Month name, full
%c    | 标准的日期的时间串 Complete date and time representation
%d    | 十进制表示的每月的第几天 Day of the month
%H    | 24小时制的小时 Hour (24-hour clock)
%I    | 12小时制的小时 Hour (12-hour clock)
%j    | 十进制表示的每年的第几天 Day of the year
%m    | 十进制表示的月份 Month number
%M    | 十时制表示的分钟数 Minute number
%S    | 十进制的秒数 Second number
%U    | 第年的第几周，把星期日做为第一天（值从0到53）Week number (Sunday first weekday)
%w    | 十进制表示的星期几（值从0到6，星期天为0）weekday number
%W    | 每年的第几周，把星期一做为第一天（值从0到53） Week number (Monday first weekday)
%x    | 标准的日期串 Complete date representation (e.g. 13/01/08)
%X    | 标准的时间串 Complete time representation (e.g. 17:02:10)
%y    | 不带世纪的十进制年份（值从0到99）Year number within century
%Y    | 带世纪部分的十制年份 Year number
%z，%Z | 时区名称，如果不能得到时区名称则返回空字符。Name of time zone
%%    | 百分号

参考:
[Python 常用函数time.strftime()简介 ](http://blog.chinaunix.net/uid-16539-id-2741508.html)