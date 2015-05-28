---
layout: post
title: "vim-pandoc基本用法"
date: 2015-05-28 10:45:20 +0800
comments: true
categories: tools
---
`vim-pandoc`可以集成Vim和Pandoc，用于写Markdown文档非常方便。

> `vim-pandoc` provides facilities to integrate Vim with the pandoc document converter 
> `vim-pandoc`'s goals are 1) to provide advanced integration with pandoc, 2) a comfortable document writing environment, and 3) great configurability.

今天试用了一下，操作过程记录如下。

# 安装Vundle
平常主要在Windows上工作和写博客，下面主要讲[Windows上如何安装Vundle](https://github.com/gmarik/Vundle.vim/wiki/Vundle-for-Windows)。(Linux平台上安装应该更简单)

安装Vundle要求先安装如下软件：

+ Git
+ Curl

通常在Windows上我们是使用[msysgit](http://msysgit.github.io/)。安装msysgit后，Git和Curl都会安装好。我的电脑上安装情况如下。

```
C:\>git --version
git version 1.9.0.msysgit.0

C:\>d:\Git\bin\curl.exe --version
curl 7.30.0 (i386-pc-win32) libcurl/7.30.0 OpenSSL/0.9.8y zlib/1.2.7
Protocols: dict file ftp ftps gopher http https imap imaps ldap ldaps pop3 pop3s
 rtsp smtp smtps telnet tftp
Features: AsynchDNS GSS-Negotiate IPv6 Largefile NTLM SPNEGO SSL SSPI libz
```

## 下载Vundle

```
cd <vim dir>
git clone https://github.com/gmarik/Vundle.vim.git vimfiles/bundle/Vundle.vim
gvim _vimrc
```

## 修改vim配置
参考[官网配置](https://github.com/gmarik/Vundle.vim/wiki/Vundle-for-Windows)在`_vimrc`中开始部分添加如下代码：

```
set nocompatible              " be iMproved, required
filetype off                  " required

" set the runtime path to include Vundle and initialize
set rtp+=$VIM/vimfiles/bundle/Vundle.vim/
let path='$VIM/vimfiles/bundle'
call vundle#begin(path)
" alternatively, pass a path where Vundle should install plugins
"call vundle#begin('~/some/path/here')

" let Vundle manage Vundle, required
Plugin 'gmarik/Vundle.vim'

" The following are examples of different formats supported.
" Keep Plugin commands between vundle#begin/end.
" plugin on GitHub repo
Plugin 'vim-pandoc/vim-pandoc'
Plugin 'vim-pandoc/vim-pandoc-syntax' 

" All of your Plugins must be added before the following line
call vundle#end()            " required
filetype plugin indent on    " required
" To ignore plugin indent changes, instead use:
"filetype plugin on
"
" Brief help
" :PluginList       - lists configured plugins
" :PluginInstall    - installs plugins; append `!` to update or just :PluginUpdate
" :PluginSearch foo - searches for foo; append `!` to refresh local cache
" :PluginClean      - confirms removal of unused plugins; append `!` to auto-approve removal
"
" see :h vundle for more details or wiki for FAQ
" Put your non-Plugin stuff after this line
```

<font color="red">注意：'set rtp'部分的配置跟官网上有所不同。</font>按官网配置VIM一直出错，提示`Unknown function: `。了解了下[什么是VIM rtp](http://vimdoc.sourceforge.net/htmldoc/options.html#%27runtimepath%27)(rtp, runtimepath)，修改为`set rtp+=$VIM/vimfiles/bundle/Vundle.vim/`后正常。

# 安装vim-pandoc
以下代码指定了我们需要安装的插件

```
Plugin 'vim-pandoc/vim-pandoc'
Plugin 'vim-pandoc/vim-pandoc-syntax'
```

运行`:PluginInstall`安装插件。

安装后运行`:Pandoc`生成指定格式的文档。我电脑上装了Python但没有加入到PATH环境变量中，结果一直出现`WindowError`。参考[这个帖子](https://github.com/vim-pandoc/vim-pandoc/issues/29)将Python加到PATH环境变量，问题解决。

# 总结
终于安装成功了，但是悲剧地发现vim-pandoc似乎并不比在[Vim自定义函数](http://zhouyichu.com/misc/Pandoc.html)方便多少(不过它的`:TOC`命令确实不错，VIM编辑Markdown时文档内导航相当方便)。可能我是vim-pandoc新手，它的强大功能还没发现。后面慢慢体会吧。

# 参考

+ [Vundle](https://github.com/gmarik/Vundle.vim)
+ [vim-pandoc](https://github.com/vim-pandoc/vim-pandoc)
