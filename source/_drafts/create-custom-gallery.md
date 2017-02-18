---
title: 自定义相册
tags:
---

```java

    private void getPhotoThumbnail() {
        //查询的列
        String[] projection = new String[]{MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_ID, // 直接包含该图片文件的文件夹ID，防止在不同下的文件夹重名
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME, // 直接包含该图片文件的文件夹名
                MediaStore.Images.Media.DISPLAY_NAME, // 图片文件名
                MediaStore.Images.Media.DATA, // 图片绝对路径
                "count(" + MediaStore.Images.Media._ID + ")"//统计当前文件夹下共有多少张图片
        };
        //这种写法是为了进行分组查询，详情可参考http://yelinsen.iteye.com/blog/836935
        String selection = " 0==0) group by bucket_display_name --(";

        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, null, "");
        PhotoFolder pf = null;
        List<PhotoFolder> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            String folderId = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID));
            String folder = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
            long fileId = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
            String finaName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            int count = cursor.getInt(5);//该文件夹下一共有多少张图片
            BitmapFactory.Options options = new BitmapFactory.Options();
            //Thumbnails.getThumbnail(cr, fileId, Thumbnails.MICRO_KIND, options)获取指定图片缩略片
            pf = new PhotoFolder(folderId, folder, count,
                    MediaStore.Images.Thumbnails.getThumbnail(cr, fileId, MediaStore.Images.Thumbnails.MICRO_KIND, options));
            list.add(pf);
        }
        if (null != cursor && !cursor.isClosed()) {
            cursor.close();
        }
    }

    private static class PhotoFolder {
        public PhotoFolder(String folderId, String folder, int count, Bitmap bitmap) {

        }
    }
```

http://www.eoeandroid.com/thread-320017-1-1.html?_dsign=0cb0bda1


http://blog.csdn.net/qq_19986309/article/details/47080287


android获取新增的图片
http://www.lai18.com/content/1263478.html

监听相册
