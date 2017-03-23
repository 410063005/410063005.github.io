---
title: rv_update_opt
tags:
---

# 问题一

如何解决抖动问题

抖动的原因是调用了 `RecyclerView.Adapter.notifyItemChanged()` 进行局部刷新

而刷新的item会使用Glide重新加载图片， 图片动画引起抖动的错觉。

如何优化。

使用 `RecyclerView.Adapter.notifyItemChanged()` 带 payload 参数的版本

并重写`public void onBindViewHolder(ViewHolder viewHolder, int position, List<Object> payloads)` 方法

```java
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position, List<Object> payloads) {
        if (payloads.contains("uv")) {
            ViewHolder holder = (ViewHolder) viewHolder;
            Posts posts = mPostsList.get(position);
            if (holder.tvRead != null) {
                holder.tvRead.setText(String.valueOf(posts.getPageview()));
            }
            if (holder.tvComment != null) {
                holder.tvComment.setText(String.valueOf(posts.getCommentnum()));
            }

        } else {
            super.onBindViewHolder(viewHolder, position, payloads);
        }
    }
```


# 问题二
前台无法判断是否新数据，只能全局刷新。

如何优化请求