http://developer.android.com/training/articles/memory.html

memory leak  
通常是因为在全局成员中持有对象的引用


android 不提供内存的 swap space，但支持 paging 和 memory-mapping


>每个 app 进程都是从一个已存在的叫 Zygote 的进行中 fork 出来的。系统启动时启动 Zygote 并加载通用的 framework code 和资源 (比如 activity theme)。 启动新的应用时，系统 fork Zygote 并在新的进程中加载和运行应用的 code。这允许为 framework code 和资源分配的 RAM page 可以在所有的应用进程中共享

大部分静态数据是通过 mmap 的方式映射到进程中。所以不仅相同的数据可以在进程中共享，而且允许这些数据被 page out。静态数据包括：Dalvik code (odex文件)，app资源，so文件


由于 android 大量使用共享内存，所以确定你的 app 到底占用了多少内存时需要很小心。这些技巧会在 [Investigating Your RAM Usage][Investigating Your RAM Usage] 中讨论。

# Allocating and Reclaiming App Memory
+ 每个进程的 Dalvik heap受到虚拟内存大小限制。定义了逻辑上的堆大小，需要时会增长(但受到系统限制)
+ 逻辑上的堆大小跟堆占用的物理内存大小并不相同。计算 app 使用堆大小时，Android 会使用一个称为 Proportional Set Size(PSS)的值。这个值考虑了跟其他进程共享的 dirty and clean pages，当然，只是按照有多少 app 共享这块内存。系统认为 PSS 是 app 占用的物理内存大小。
+ android 并不减除堆上的碎片，所以也不会压缩堆的逻辑大小。只有堆的尾部有未使用的空间时 android 才会减少堆大小。但这也不意味着堆占用的物理内存不会被压缩。垃圾收集后，Dalvik 会从堆中找到未使用的 pages，然后使用 `madvise` 将这些 pages 归还给内核。

# Restricting App Memory
为了维持一个正常的多任务环境，android 对每个 app 使用的堆大小设置了一个硬指标。具体的值在不同设备间并不一样，主要跟设备 RAM 多大相关。如果你的 app 已经占满了整个堆，再尝试分配更多内存时，将抛出 OOM。

# Switching Apps
上面说到，Android 没有 swap space，android 将所有的非前台进程 (即这个进程没有 host 一个前台app component，所以用户不可见)放到一个 LRU Cache 中。比如，用户首次启动一个 app 时，将启动一个进程用于运行这个 app。当用户离开这个 app 时，进程并不会退出。系统会缓存这个进程，所以当用户之后又回到这个应用时，会重用原来的进程，以达到 app 快速切换。

---
如果 app 有缓存进程，它占了内存但其实并不使用。甚至用户都不使用这个 app 。这当然会限制系统总体性能。当系统内存过低时，它可能会杀死 LRU cache 中的进程，从最近最少使用的进程开始，当然，会同时考虑哪个进程是消耗最多内存的。想让你的进程尽可能长时间地被缓存，请参考以下关于何时释放引用的建议。

# How Your App Should Manage Memory
## 少用 service
如果 app 需要 service 在后台工作，任务完成后不要让它保持运行。

系统倾向于一直保持 service 正在运行的进程。这会让这个进程代价非常大，因为 service 占用的 RAM 无法用于其他目的，也不能 page out。系统在 LRU Cache 中能保存的进程更少了，而 app 的切换也变低效了。

>Leaving a service running when it’s not needed is one of the worst memory-management mistakes an Android app can make

## 用户接口隐藏后及时释放内存
onTrimMemory

这个 api 在 level 14 中开始加入，在之前的版本中可以使用 `onLowMemory()` 作为替代。这个老的 api 基本等同于 TRIM_MEMORY_COMPLETE

系统杀死 LRU Cache 中的进程时，是从下往上进行的，但它还会考虑每个进程占用的内存多少。你在 LRU list 中占用的内存相对越少，被杀死的可能性也越小。

## 检查该用多少内存

可以使用 `getMemoryClass()` 方法来估算你的应用还有多少兆可用的堆内存。

可以将 <application> 的 largeHeap 属性设置为 `true`， 来要求更大的堆内存。
>Never request a large heap simply because you've run out of memory 

另外，就算你的 app 内存使用正常，不会引起 OOM，你也应尽可能少地占用内存，因为内存越多 GC 时间也越长，这会影响用户体验。

## 避免 bitmap 浪费内存
仅使用足够的分辨率即可

## 


http://developer.android.com/tools/debugging/debugging-memory.html

[Investigating Your RAM Usage]: http://developer.android.com/tools/debugging/debugging-memory.html