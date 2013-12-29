
[Warning shows when i use Hash Map In android(Use new SparseArray<String>)][sparseArray]



[SparseArray.html][SparseArray.html]

>The implementation is not intended to be appropriate for data structures that may contain large numbers of items. It is generally slower than a traditional HashMap, since lookups require a binary search and adds and removes require inserting and deleting entries in the array. For containers holding up to hundreds of items, the performance difference is not significant, less than 50%.

并不一定更快。实际上通常更慢。但可以减少自动装箱产生的临时对象。

另外，
>
another  advantage of SparseArray -- it doesn't need to allocate an extra entry object for every mapping it contains, nor resize a hash table as the amount of items changes

这个来自[android-developers][android-developers]


[sparseArray]: http://stackoverflow.com/questions/14787785/warning-shows-when-i-use-hash-map-in-androiduse-new-sparsearraystring

[SparseArray.html]: http://developer.android.com/reference/android/util/SparseArray.html

[android-developers]: https://groups.google.com/forum/?fromgroups=#!topic/android-developers/G-ceop4xp48

[Android开发性能优化之SparseArray和HashMap]: http://my.eoe.cn/appadventure/archive/2824.html