    @Override
    public void onGetCollege(CollegeResponse result) {
        // if (getActivity() == null) {
        //    return;
        // }
        if (result == null || result.getColleges().size() <= 0) {
            return;
        }
        collegeList = result.getColleges();
        initCollegeFilter();

    }
```

```
@Override
public void onLoadData(BaseResponse result) {
	super.onLoadData(result);
	mPullToRefreshListView.onRefreshComplete();
	dismissProgressDialog();
	// getActivity()可能返回null!!
	String label = DateUtils.formatDateTime(getActivity().getApplicationContext(), System.currentTimeMillis(),
	...
}
```

## response.getRspData()返回null

```
MessageResponse response = queryUserFollowInfo(orderId, orderCtm, users);
QueryUserFollowInfoRsp queryUserFollowInfoRsp = (QueryUserFollowInfoRsp) (response.getRspData());
if (queryUserFollowInfoRsp != null) {
	List<Follower> followerList = queryUserFollowInfoRsp.followers;

	for (int i = 0; i < followerList.size(); i++) {
		Follower follower = followerList.get(i);
		followerMap.put(follower.follower.uid, 1);
	}
}
```

# 其他错误

```
//汉字转换成拼音
String pinyin = "";
try {
	pinyin = CharacterParser.getInstance().getSelling(department1.getName());
} catch (Throwable t) {
	Logger.e(t.toString());
}
```
这里在某些机型上可能出现错误，所以需要捕获`Throwable`
