package com.bolaa.sleepingbar.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bolaa.sleepingbar.R;
import com.bolaa.sleepingbar.adapter.TopicCommentsAdapter;
import com.bolaa.sleepingbar.common.APIUtil;
import com.bolaa.sleepingbar.common.AppStatic;
import com.bolaa.sleepingbar.common.AppUrls;
import com.bolaa.sleepingbar.controller.LoadStateController;
import com.bolaa.sleepingbar.httputil.HttpRequester;
import com.bolaa.sleepingbar.httputil.ParamBuilder;
import com.bolaa.sleepingbar.model.Information;
import com.bolaa.sleepingbar.model.Topic;
import com.bolaa.sleepingbar.model.TopicComments;
import com.bolaa.sleepingbar.model.wrapper.BeanWraper;
import com.bolaa.sleepingbar.model.wrapper.CommentsWraper;
import com.bolaa.sleepingbar.parser.gson.BaseObject;
import com.bolaa.sleepingbar.parser.gson.GsonParser;
import com.bolaa.sleepingbar.utils.AppUtil;
import com.bolaa.sleepingbar.utils.Image13Loader;
import com.bolaa.sleepingbar.view.ResizeLinearLayout;
import com.bolaa.sleepingbar.view.pulltorefresh.PullListView;
import com.bolaa.sleepingbar.view.pulltorefresh.PullToRefreshBase;
import com.core.framework.app.devInfo.ScreenUtil;
import com.core.framework.net.NetworkWorker;
import com.core.framework.net.NetworkWorker.ICallback;
import com.core.framework.util.DialogUtil;
import com.core.framework.util.IOSDialogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.List;

/**
 * 社区资讯详情页面
 */
public class InformationActivity extends BaseListActivity implements
		PullToRefreshBase.OnRefreshListener, LoadStateController.OnLoadErrorListener {

	private View header;
	private TextView btnPublish;
	private Information posts;


	EditText etComment;
	ResizeLinearLayout rootLayout;

	private TextView tvName;
//	private TextView tvContent;
	private TextView tvDate;
	private TextView tvAccessCount;
	private ImageView ivPic;

	private String postsId;
	private int curIv = 0;
	private int posts_position = -1;//从帖子列表进来的，再列表中的索引

	WebView mWebView;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setExtra();
		initView();
		initPosts();
		setListener();
		loadPosts();
	}

	private void setExtra() {
		// TODO Auto-generated method stub
		Intent intent = getIntent();
		postsId = intent.getStringExtra("posts_id");
		posts_position=intent.getIntExtra("posts_position", -1);
		Bundle data = intent.getBundleExtra("data");
	}

	private void initView() {
		setActiviyContextView(R.layout.activity_bbs_posts_detail, true, true);
		setTitleText("", "全文", 0, true);
		etComment = (EditText) findViewById(R.id.et_bbs_posts_comment);
		rootLayout = (ResizeLinearLayout) findViewById(R.id.layout_root);
		btnPublish = (TextView) findViewById(R.id.btn_publish);

		mPullListView = (PullListView) findViewById(R.id.pull_listview);
        mPullListView.setMode(PullToRefreshBase.MODE_PULL_DOWN_TO_REFRESH);
		mPullListView.setOnRefreshListener(this);
		mListView = mPullListView.getRefreshableView();
		header = View.inflate(this, R.layout.layout_information_detail_header, null);
		mListView.addHeaderView(header);
		mAdapter = new TopicCommentsAdapter(this);
		mListView.setAdapter(mAdapter);
	}

	private void initPosts() {
		// TODO Auto-generated method stub
		tvName=(TextView)header.findViewById(R.id.tv_name);
		mWebView =(WebView) header.findViewById(R.id.tv_content);
		initWebView();
//		tvContent =(TextView)header.findViewById(R.id.tv_content);
		tvDate =(TextView)header.findViewById(R.id.tv_date);
		tvAccessCount =(TextView)header.findViewById(R.id.tv_access_count);
		tvDate =(TextView)header.findViewById(R.id.tv_date);
		ivPic=(ImageView) header.findViewById(R.id.iv_pic);
		ViewGroup.LayoutParams lp=ivPic.getLayoutParams();
		lp.height=(int)(0.3958 * ScreenUtil.WIDTH);
		ivPic.setLayoutParams(lp);
	}

	private void initWebView() {
		// LayoutParams lp=new
		// LayoutParams(LayoutParams.MATCH_PARENT,ScreenUtil.HEIGHT);
		// mWebView.setLayoutParams(lp);
		// contentInSV.addView(mWebView);
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        int width = wm.getDefaultDisplay().getWidth();
        if(width > 650)
        {
            this.mWebView.setInitialScale(190);
        }else if(width > 520)
        {
            this.mWebView.setInitialScale(160);
        }else if(width > 450)
        {
            this.mWebView.setInitialScale(140);
        }else if(width > 300)
        {
            this.mWebView.setInitialScale(120);
        }else
        {
            this.mWebView.setInitialScale(100);
        }
        WebSettings webSettings = mWebView.getSettings();

		if(ScreenUtil.DENSITY_DPI>520){
			webSettings.setTextSize(WebSettings.TextSize.LARGEST);
		}else if(ScreenUtil.DENSITY_DPI>360){
			webSettings.setTextSize(WebSettings.TextSize.LARGER);
		}else if(ScreenUtil.DENSITY_DPI>200){
			webSettings.setTextSize(WebSettings.TextSize.NORMAL);
		}else {
			webSettings.setTextSize(WebSettings.TextSize.SMALLEST);
		}


		webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
		mWebView.setWebViewClient(new WebViewClient() {

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// TODO Auto-generated method stub
				view.loadUrl(url);
				return true;
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				// TODO Auto-generated method stub
				super.onPageFinished(view, url);
				mWebView.setVisibility(View.VISIBLE);
			}
		});
	}


	private void setPostsView() {
		tvName.setText(posts.title);
		if(AppUtil.isNull(posts.content)){
//			tvContent.setText("");
			mWebView.setVisibility(View.GONE);
		}else {
			/*new Thread(new Runnable() {
				@Override
				public void run() {
					final Spanned spanned=Html.fromHtml(posts.content, imgGetter, null);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							tvContent.setText(spanned);
						}
					});
				}
			}).start();
//			tvContent.setText(Html.fromHtml(posts.content, imgGetter, null));*/
			mWebView.loadData(posts.content, "text/html;charset=UTF-8", null);
		}
		tvDate.setText(posts.add_time);
		tvAccessCount.setText("浏览量："+posts.page_view);
		Image13Loader.getInstance().loadImageFade(posts.file_url,ivPic);
	}

	Html.ImageGetter imgGetter = new Html.ImageGetter() {
		public Drawable getDrawable(String source) {
			Drawable drawable = null;
			URL url;
			try {
				url = new URL(source);
				drawable = Drawable.createFromStream(url.openStream(), ""); // 获取网路图片
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
//			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
//					drawable.getIntrinsicHeight());
			return drawable;
		}
	};

	//检查数据库
	public static void struct() {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads().detectDiskWrites().detectNetwork() // or
				// .detectAll()
				// for
				// all
				// detectable
				// problems
				.penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
				.detectLeakedSqlLiteObjects() // 探测SQLite数据库操作
				.penaltyLog() // 打印logcat
				.penaltyDeath().build());
	}

//	private void addTitleHeader() {
//		header2 = new TextView(this);
//		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
//				AbsListView.LayoutParams.WRAP_CONTENT,
//				AbsListView.LayoutParams.WRAP_CONTENT);
//		header2.setLayoutParams(lp);
//		header2.setPadding(ScreenUtil.dip2px(this, 10),
//				ScreenUtil.dip2px(this, 10), ScreenUtil.dip2px(this, 10),
//				ScreenUtil.dip2px(this, 10));
//		header2.setCompoundDrawablePadding(ScreenUtil.dip2px(this, 5));
////		header2.setCompoundDrawablesWithIntrinsicBounds(
////				R.drawable.shape_rectangle_vertical_yellow, 0, 0, 0);
//		header2.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources()
//				.getDimensionPixelSize(R.dimen.text_size));
//		header2.setTextColor(getResources().getColor(R.color.text_grey));
//		header2.setText("评论");
//		mListView.addHeaderView(header2);
//	}

	private void setListener() {
        mPullListView.setOnRefreshListener(this);
		mListView.setOnScrollListener(new MyOnScrollListener());
		btnPublish.setOnClickListener(this);
		rootLayout.setOnResizeListener(new ResizeLinearLayout.OnResizeListener() {

			@Override
			public void OnResize(int w, int h, int oldw, int oldh) {
				// TODO Auto-generated method stub

			}
		});

		((TopicCommentsAdapter)mAdapter).setOnShowMenuListener(new TopicCommentsAdapter.OnShowMenuListener() {
			@Override
			public void onShow(TopicComments comments) {
				showMenu(comments);
			}
		});
	}

	private void initData(boolean isRefresh) {

		if (!isRefresh) {
			// showLoading();
		}
		ParamBuilder params = new ParamBuilder();
		params.append("id", postsId + "");
		params.append("type", 1);
		if (isRefresh) {
			immediateLoadData(APIUtil.parseGetUrlHasMethod(params.getParamList(),AppUrls.getInstance().URL_TOPIC_COMMENTS_LIST),
					CommentsWraper.class);
		} else {
			reLoadData(APIUtil.parseGetUrlHasMethod(params.getParamList(),AppUrls.getInstance().URL_TOPIC_COMMENTS_LIST),
					CommentsWraper.class);
		}
	}

	@Override
	protected BeanWraper newBeanWraper() {
		return new CommentsWraper();
	}

	private void loadPosts() {
		showLoading();
		ParamBuilder params=new ParamBuilder();
		params.append("id", postsId);
		NetworkWorker.getInstance().get(APIUtil.parseGetUrlHasMethod(params.getParamList(),AppUrls.getInstance().URL_INFORMATION_DETAIL), new ICallback() {

					@Override
					public void onResponse(int status, String result) {
						// TODO Auto-generated method stub
						if (status == 200) {
							BaseObject<Information> object = GsonParser.getInstance().parseToObj(result, Information.class);
							if (object != null && object.status == BaseObject.STATUS_OK && object.data != null) {
								showSuccess();
								posts = object.data;
								setPostsView();
								initData(false);
							} else {
								showNodata();
							}
						} else {
							showFailture();
						}
					}
				});

	}

	@Override
	protected void handlerData(List allData, List currentData,
			boolean isLastPage) {
		// TODO Auto-generated method stub
		mPullListView.onRefreshComplete();
		if (AppUtil.isEmpty(allData)) {
			// AppUtil.showToast(this, "暂无评论");
			return;
		}
		mAdapter.setList(allData);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	protected void loadError(String message, Throwable throwable, int page) {
		// TODO Auto-generated method stub
		mPullListView.onRefreshComplete();
	}

	@Override
	protected void loadTimeOut(String message, Throwable throwable) {
		// TODO Auto-generated method stub
		mPullListView.onRefreshComplete();
	}

	@Override
	protected void loadNoNet() {
		// TODO Auto-generated method stub

		mPullListView.onRefreshComplete();
		AppUtil.showToast(this, "请检查网络");
	}

	@Override
	protected void loadServerError() {
		// TODO Auto-generated method stub
		mPullListView.onRefreshComplete();
		AppUtil.showToast(this, "连接失败");
	}

	@Override
	public void onRefresh() {
		// TODO Auto-generated method stub
		if (!isLoading()) {
			initData(true);
		}
	}

	private void closeInputMethod() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		boolean isOpen = imm.isActive();
		if (isOpen) {
			// imm.toggleSoftInput(0,
			// InputMethodManager.HIDE_NOT_ALWAYS);//没有显示则显示
			imm.hideSoftInputFromWindow(etComment.getWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub'
		if (v == btnPublish) {
			if (AppStatic.getInstance().isLogin) {
				publishComment();
			} else {
				UserLoginActivity.invoke(this);
			}
		}
//		else if (v == ivCollection) {
//			if (AppStatic.getInstance().isLogin) {
//				collection();
//			} else {
//				UserLoginActivity.invoke(this);
//			}
//		}else if (v == tvInform) {
//			// 举报
//			if (AppStatic.getInstance().isLogin) {
//				inform();
//			} else {
//				UserLoginActivity.invoke(this);
//			}
//		}
		else {
			super.onClick(v);
		}
	}
	
	
	private void sendStickyUpdateNotify(int type,int count){
		if(posts_position<0||type<=0)return;
		Intent intent =new Intent();
		if(type==1){//点赞
			intent.setAction("com.bolaa.cang.ACTION.POSTS.UPDATE.PRAISE");
		}else if (type==2) {//评论了
			intent.setAction("com.bolaa.cang.ACTION.POSTS.UPDATE.RECOMMENT");
		}
		intent.putExtra("target_count", count);
		intent.putExtra("target_id", posts!=null?posts.article_id:(postsId+""));
		intent.putExtra("posts_position", posts_position);
		sendStickyBroadcast(intent);
	}

	private void inform() {
		DialogUtil.showDialog(lodDialog);
		HttpRequester httpRequester = new HttpRequester();
		httpRequester.getParams().put(ParamBuilder.ACCESS_TOKEN,
				NetworkWorker.getInstance().ACCESS_TOKEN);
		httpRequester.getParams().put("id", "" + posts.article_id);
		httpRequester.getParams().put("content", posts.content);

		NetworkWorker.getInstance().post(
				AppUrls.getInstance().URL_BBS_POSTS_INFORM, new ICallback() {

					@Override
					public void onResponse(int status, String result) {
						// TODO Auto-generated method stub
						if (!isFinishing())
							DialogUtil.dismissDialog(lodDialog);
						if (status == 200) {
							BaseObject<String> object = GsonParser.getInstance().parseToObj(result,Object.class);
							if (object != null && object.status == BaseObject.STATUS_OK) {
								AppUtil.showToast(getApplicationContext(), object.info);
							} else {
								AppUtil.showToast(getApplicationContext(), object != null ? object.info : "操作失败");
							}
						} else {
							AppUtil.showToast(getApplicationContext(), "操作失败");
						}
					}
				}, httpRequester);
	}

//	private void clickGood() {
//		if (posts.is_praise == 1) {
//			AppUtil.showToast(this, "您已点过赞");
//			return;
//		}
//		ParamBuilder params = new ParamBuilder();
//        params.append("id", "" + posts.id);
//		NetworkWorker.getInstance().get(APIUtil.parseGetUrlHasMethod(params.getParamList(),AppUrls.getInstance().URL_BBS_POSTS_GOOD), new ICallback() {
//
//					@Override
//					public void onResponse(int status, String result) {
//						// TODO Auto-generated method stub
//						if (status == 200) {
//							BaseObject<String> object = GsonParser
//									.getInstance().parseToObj(result,
//											Object.class);
//							if (object != null && object.status == BaseObject.STATUS_OK) {
//								posts.praise_num = posts.praise_num + 1;
//                                posts.is_praise=1;
//                                ivPraise.setImageResource(R.drawable.ic_heart_purple);
//								tvPraiseCount.setText("" + posts.praise_num);
////								sendStickyUpdateNotify(1, posts.praise_num);
//							} else {
//								AppUtil.showToast(getApplicationContext(), object != null ? object.info : "操作失败");
//							}
//						} else {
//							AppUtil.showToast(getApplicationContext(), "操作失败");
//						}
//					}
//				});
//	}

	private void publishComment() {
		if (posts == null)
			return;
		if (!AppStatic.getInstance().isLogin) {
			UserLoginActivity.invokeForResult(this, 1);
			return;
		}
		String comment = etComment.getText().toString();
		if (comment == null || comment.trim().length() <= 0) {
			AppUtil.showToast(this, "评论不能为空");
			return;
		}else if(comment.length() > 300){
			AppUtil.showToast(this, "评论字数不能超过300");
			return;
		}
		HttpRequester requester = new HttpRequester();
		requester.getParams().put("id", "" + postsId);
		requester.getParams().put("content", comment);
		requester.getParams().put("type", "1");//资讯评论

		NetworkWorker.getInstance().post(AppUrls.getInstance().URL_PUBLISH_COMMENTS, new ICallback() {

					@Override
					public void onResponse(int status, String result) {
						// TODO Auto-generated method stub
						if (status == 200) {
							JSONObject jsonObject;
							try {
								jsonObject = new JSONObject(result);
								int code = jsonObject.optInt("status");
								if (code == 1) {// 成功
									initData(true);
									closeInputMethod();
									etComment.clearFocus();
									etComment.setText("");
								}
								AppUtil.showToast(getApplicationContext(),
										jsonObject.optString("info"));
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								AppUtil.showToast(getApplicationContext(),
										"评论失败");
							}
						} else {
							AppUtil.showToast(getApplicationContext(), "评论失败");
						}
					}
				}, requester);

	}

	private void showMenu(final TopicComments topic){
		if(topic.has_been_cared==1){//已经被关注
			new IOSDialogUtil(this).builder().setCancelable(true).setCanceledOnTouchOutside(true)
					.addSheetItem("取消关注", IOSDialogUtil.SheetItemColor.Purple, new IOSDialogUtil.OnSheetItemClickListener() {
						@Override
						public void onClick(int which) {
							cancelCare(topic);
						}
					}).addSheetItem("举报", IOSDialogUtil.SheetItemColor.Red, new IOSDialogUtil.OnSheetItemClickListener() {
				@Override
				public void onClick(int which) {
					inform(topic);
				}
			}).show();
		}else {
			new IOSDialogUtil(this).builder().setCancelable(true).setCanceledOnTouchOutside(true)
					.addSheetItem("关注Ta", IOSDialogUtil.SheetItemColor.Purple, new IOSDialogUtil.OnSheetItemClickListener() {
						@Override
						public void onClick(int which) {
							doCare(topic,1);
						}
					}).addSheetItem("举报", IOSDialogUtil.SheetItemColor.Red, new IOSDialogUtil.OnSheetItemClickListener() {
				@Override
				public void onClick(int which) {
					inform(topic);
				}
			}).show();
		}
	}

	private void doCare(final TopicComments friends , int type){
		DialogUtil.showDialog(lodDialog);
		ParamBuilder params=new ParamBuilder();
		params.append("f_type",type);
		params.append("f_user_id",friends.user_id);
		NetworkWorker.getInstance().get(APIUtil.parseGetUrlHasMethod(params.getParamList(), AppUrls.getInstance().URL_DO_CARE), new NetworkWorker.ICallback() {
			@Override
			public void onResponse(int status, String result) {
				if(!isFinishing())DialogUtil.dismissDialog(lodDialog);
				if(status==200){
					BaseObject<Object> obj=GsonParser.getInstance().parseToObj(result,Object.class);
					if(obj!=null){
						if(obj.status==BaseObject.STATUS_OK){
                            ((TopicCommentsAdapter)mAdapter).setCaredStatusByUid(friends.user_id,1);
                            AppUtil.showToast(getApplicationContext(),obj.info);
                        }else {
							AppUtil.showToast(getApplicationContext(),obj.info);
						}
					}else {
						AppUtil.showToast(getApplicationContext(),"解析出错");
					}
				}else {
					AppUtil.showToast(getApplicationContext(),"请检查网络");
				}
			}
		});
	}

	private void cancelCare(final TopicComments friends){
		DialogUtil.showDialog(lodDialog);
		ParamBuilder params=new ParamBuilder();
		params.append("f_user_id",friends.user_id);
		params.append("tab","me_care");
		NetworkWorker.getInstance().get(APIUtil.parseGetUrlHasMethod(params.getParamList(), AppUrls.getInstance().URL_CANCEL_CARE), new NetworkWorker.ICallback() {
			@Override
			public void onResponse(int status, String result) {
				if(!isFinishing())DialogUtil.dismissDialog(lodDialog);
				if(status==200){
					BaseObject<Object> obj=GsonParser.getInstance().parseToObj(result,Object.class);
					if(obj!=null){
						if(obj.status==BaseObject.STATUS_OK){
                            ((TopicCommentsAdapter)mAdapter).setCaredStatusByUid(friends.user_id,0);
                            AppUtil.showToast(getApplicationContext(),obj.info);
                        }else {
							AppUtil.showToast(getApplicationContext(),obj.info);
						}
					}else {
						AppUtil.showToast(getApplicationContext(),"解析出错");
					}
				}else {
					AppUtil.showToast(getApplicationContext(),"请检查网络");
				}
			}
		});
	}

	private void inform(final TopicComments topic){
		DialogUtil.showDialog(lodDialog);
		ParamBuilder params=new ParamBuilder();
		params.append("o_id",topic.id);
		params.append("r_type",3);
		NetworkWorker.getInstance().get(APIUtil.parseGetUrlHasMethod(params.getParamList(), AppUrls.getInstance().URL_BBS_POSTS_INFORM), new NetworkWorker.ICallback() {
			@Override
			public void onResponse(int status, String result) {
				if(!isFinishing())DialogUtil.dismissDialog(lodDialog);
				if(status==200){
					BaseObject<Object> obj=GsonParser.getInstance().parseToObj(result,Object.class);
					if(obj!=null){
						if(obj.status==BaseObject.STATUS_OK){
                            AppUtil.showToast(getApplicationContext(),obj.info);
						}else {
							AppUtil.showToast(getApplicationContext(),obj.info);
						}
					}else {
						AppUtil.showToast(getApplicationContext(),"解析出错");
					}
				}else {
					AppUtil.showToast(getApplicationContext(),"请检查网络");
				}
			}
		});
	}

//	private void collection() {
//		if (posts == null)
//			return;
//		if (!AppStatic.getInstance().isLogin) {
//			UserLoginActivity.invokeForResult(this, 1);
//			return;
//		}
//
//		HttpRequester requester = new HttpRequester();
//		requester.getParams().clear();
//		requester.getParams().put(ParamBuilder.ACCESS_TOKEN, NetworkWorker.getInstance().ACCESS_TOKEN);
//		requester.getParams().put(ParamBuilder.BBS_ID, "" + posts.id);
//		NetworkWorker.getInstance().post(
//				AppUrls.getInstance().URL_BBS_POSTS_COLLECTION,
//				new ICallback() {
//
//					@Override
//					public void onResponse(int status, String result) {
//						// TODO Auto-generated method stub
//						if (status == 200) {
//							JSONObject jsonObject;
//							try {
//								jsonObject = new JSONObject(result);
//								int code = jsonObject.optInt("status");
//								if (code == 1) {// 成功
//									if (posts.is_collection == 0) {
//										posts.is_collection = 1;
//									} else {
//										posts.is_collection = 0;
//									}
//									ivCollection
//											.setImageResource(posts.is_collection == 1 ? R.drawable.ic_level_star2
//													: R.drawable.ic_level_star);
//								}
//								AppUtil.showToast(getApplicationContext(),
//										jsonObject.optString("info"));
//							} catch (JSONException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//								AppUtil.showToast(getApplicationContext(),
//										"操作失败");
//							}
//						} else {
//							AppUtil.showToast(getApplicationContext(), "操作失败");
//						}
//					}
//				}, requester);
//
//	}

	// @Deprecated
	// private void showShareWindow() {
	// if (shareWindow == null) {
	// View view = LayoutInflater.from(this).inflate(
	// R.layout.dialog_share_posts, null);
	// view.findViewById(R.id.tv_cancel).setOnClickListener(
	// new OnClickListener() {
	//
	// @Override
	// public void onClick(View v) {
	// // TODO Auto-generated method stub
	// if (!isFinishing()) {
	// shareWindow.dismiss();
	// }
	// }
	// });
	//
	// List<ShareChannel> list = new ArrayList<ShareChannel>();
	// ShareChannel channel = new ShareChannel(0, "qq好友",
	// R.drawable.ic_share_qq);
	// list.add(channel);
	// channel = new ShareChannel(1, "qq空间", R.drawable.ic_share_qq_zore);
	// list.add(channel);
	// channel = new ShareChannel(2, "微信好友", R.drawable.ic_share_wx_friend);
	// list.add(channel);
	// channel = new ShareChannel(3, "新浪微博", R.drawable.ic_share_weibo);
	// list.add(channel);
	// channel = new ShareChannel(4, "朋友圈", R.drawable.ic_share_wx_center);
	// list.add(channel);
	// final ShareGridAdapter shareGridAdapter = new ShareGridAdapter(this);
	// shareGridAdapter.setList(list);
	// ((GridView) view.findViewById(R.id.gridview))
	// .setAdapter(shareGridAdapter);
	// ((GridView) view.findViewById(R.id.gridview))
	// .setOnItemClickListener(new OnItemClickListener() {
	//
	// @Override
	// public void onItemClick(AdapterView<?> parent,
	// View view, int position, long id) {
	// // TODO Auto-generated method stub
	// ShareChannel sChannel = (ShareChannel) shareGridAdapter
	// .getItem(position);
	// sChannel.share(BBSPostsDetailActivity.this,
	// "http://www.baidu.com", null, null, null);
	// shareWindow.dismiss();
	// }
	// });
	//
	// shareWindow = DialogUtil.getMenuDialog(this, view);
	// shareWindow.show();
	// } else {
	// shareWindow.show();
	// }
	// }

	/**
	 * 掉这个方法启动页面，就重新加载帖子内容
	 * 
	 * @param context
	 * @param postsId
	 */
	public static void invoke(Context context, String postsId) {
		Intent intent = new Intent(context, InformationActivity.class);
		intent.putExtra("posts_id", postsId);
		context.startActivity(intent);
	}
	
	public static void invoke(Context context, String postsId,int position) {
		Intent intent = new Intent(context, InformationActivity.class);
		intent.putExtra("posts_id", postsId);
		intent.putExtra("posts_position", position);
		context.startActivity(intent);
	}

	@Override
	public void onAgainRefresh() {
		// TODO Auto-generated method stub
		initData(false);
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
	}
}
