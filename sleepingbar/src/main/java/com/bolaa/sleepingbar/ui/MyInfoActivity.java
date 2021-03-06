package com.bolaa.sleepingbar.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bolaa.sleepingbar.HApplication;
import com.bolaa.sleepingbar.R;
import com.bolaa.sleepingbar.base.BaseActivity;
import com.bolaa.sleepingbar.common.APIUtil;
import com.bolaa.sleepingbar.common.AppStatic;
import com.bolaa.sleepingbar.common.AppUrls;
import com.bolaa.sleepingbar.httputil.HttpRequester;
import com.bolaa.sleepingbar.httputil.ParamBuilder;
import com.bolaa.sleepingbar.model.UserInfo;
import com.bolaa.sleepingbar.parser.gson.BaseObject;
import com.bolaa.sleepingbar.parser.gson.GsonParser;
import com.bolaa.sleepingbar.utils.AppUtil;
import com.bolaa.sleepingbar.utils.DateTimeUtils;
import com.bolaa.sleepingbar.utils.DateUtil;
import com.bolaa.sleepingbar.utils.ImageUtil;
import com.bolaa.sleepingbar.view.CircleImageView;
import com.bolaa.sleepingbar.view.wheel.NumericWheelAdapter;
import com.bolaa.sleepingbar.view.wheel.OnWheelScrollListener;
import com.bolaa.sleepingbar.view.wheel.WheelView;
import com.bolaa.sleepingbar.watch.WatchConstant;
import com.bolaa.sleepingbar.watch.WatchService;
import com.core.framework.app.devInfo.ScreenUtil;
import com.core.framework.develop.LogUtil;
import com.core.framework.image.universalimageloader.core.ImageLoader;
import com.core.framework.net.NetworkWorker;
import com.core.framework.net.NetworkWorker.ICallback;
import com.core.framework.store.sharePer.PreferencesUtils;
import com.core.framework.util.DialogUtil;
import com.core.framework.util.IOSDialogUtil;
import com.core.framework.util.IOSDialogUtil.OnSheetItemClickListener;
import com.core.framework.util.IOSDialogUtil.SheetItemColor;
import com.core.framework.util.StringUtil;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * 基本信息
 * 
 * @author paulz
 * 
 */
public class MyInfoActivity extends BaseActivity {
	private final static int TAKE_PHOTO = 1;// 拍照
	private final static int TAKE_PICTURE = 2;// 本地获取
	private final static int TAKE_CROP = 3;// 裁剪
	private final static int RE_REGION = 4;// 裁剪
	private final static int RE_NAME = 5;// 裁剪
	
	private boolean isModifyMode;

	private CircleImageView mIconIv;
	private TextView mNameTv;
	private TextView tvUploadAvatar;
	private TextView mBirthTv;
	private EditText mNameEt;
	private EditText mWeightEt;
	private EditText mHeightEt;
	private TextView mSexTv;
	private TextView mHeightTv;
	private TextView mWeightTv;
	private LinearLayout mTimeLayout;

	private WheelView year;
	private WheelView month;
	private WheelView day;

	private int mYear = 2010;
	private int mMonth = 3;//min=0
	private int mDay = 3;
	private View view;
	private String mBirthday;
	private Dialog mDialog;

	private String mFilePath;
	private UserInfo mUserInfo;
	
	private String real_name;
	private String blood;
	private String id_card;
	private String sex="1";
	private File avatar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setActiviyContextView(R.layout.activity_myinfo);
		setTitleTextRightText("", "我的信息", "修改", true);
		mBirthday = "1900-01-01";
		initView();
		setListener();
        initData();
        loadPageInfo(false);
		mDialog = DialogUtil.getMenuDialog2(this, getDataPick(), ScreenUtil.getScreenWH(this)[1] / 2);
		mDialog.setCanceledOnTouchOutside(true);
	}

	private void initView() {
		mIconIv = (CircleImageView) findViewById(R.id.myInfo_iconIv);
		mHeightTv = (TextView) findViewById(R.id.tv_height);
		mHeightEt = (EditText) findViewById(R.id.et_height);
		mNameTv = (TextView) findViewById(R.id.tv_name);
		mBirthTv = (TextView) findViewById(R.id.tv_birthday);
		mSexTv = (TextView) findViewById(R.id.tv_sex);
		mWeightTv = (TextView) findViewById(R.id.tv_weight);
		mWeightEt = (EditText) findViewById(R.id.et_weight);
		mNameEt = (EditText) findViewById(R.id.et_name);
		tvUploadAvatar = (TextView) findViewById(R.id.tv_upload_avatar);
		mIconIv.setEnabled(false);
	}
	
	private void setListener() {
		// TODO Auto-generated method stub
		mSexTv.setOnClickListener(this);
		mIconIv.setOnClickListener(this);
		mBirthTv.setOnClickListener(this);
	}

    private void loadPageInfo(final boolean beforeModify){
        DialogUtil.showDialog(lodDialog);
        ParamBuilder params = new ParamBuilder();
        NetworkWorker.getInstance().get(APIUtil.parseGetUrlHasMethod(params.getParamList(),AppUrls.getInstance().URL_USER_PAGE_INFO), new NetworkWorker.ICallback() {

            @Override
            public void onResponse(int status, String result) {
                if (!isFinishing()) {
                    DialogUtil.dismissDialog(lodDialog);
                }
                if(status==200){
                    BaseObject<UserInfo> object= GsonParser.getInstance().parseToObj(result, UserInfo.class);
                    if(object!=null){
                        if(object.data!=null&&object.status==BaseObject.STATUS_OK){
                            if(beforeModify){
                                AppUtil.showToast(getApplicationContext(), "修改成功");
                                switchMode();
                            }
                            AppStatic.getInstance().updateUserPartly(object.data);
                            AppStatic.getInstance().setmUserInfo(AppStatic.getInstance().getUser());
                            initData();
                        }else {
                            AppUtil.showToast(getApplicationContext(), object.info);
                        }
                    }else {
                        AppUtil.showToast(getApplicationContext(), "请检查网络");
                    }
                }

            }
        });
    }

	private void initData() {
		mUserInfo = AppStatic.getInstance().getmUserInfo();
		if (mUserInfo != null) {
			mIconIv.setUrl(mUserInfo.avatar);
			setText(mUserInfo.nick_name, mNameTv);
			setText(mUserInfo.nick_name, mNameEt);
			setText(mUserInfo.height, mHeightEt);
			setText(mUserInfo.height, mHeightTv);
			setText(mUserInfo.weight, mWeightEt);
			setText(mUserInfo.weight, mWeightTv);
			setText(mUserInfo.birthday, mBirthTv);
			mBirthday=mUserInfo.birthday;
			sex=mUserInfo.sex;
			if (mUserInfo.sex.equals("1")) {
				mSexTv.setText("男");
			} else if (mUserInfo.sex.equals("2")) {
				mSexTv.setText("女");
			} else {
				mSexTv.setText("男");
			}
			//设置时间选择弹框的初始值
			if(!AppUtil.isNull(mUserInfo.birthday)){
				String[] dateStrs=mUserInfo.birthday.split("-");
				if(dateStrs.length>=3){
					mYear=Integer.valueOf(dateStrs[0]);
					mMonth=Integer.valueOf(dateStrs[1])-1;
					mDay=Integer.valueOf(dateStrs[2]);
				}else {
					mYear=Integer.valueOf(dateStrs[0]);
				}
			}
		} else {
			mIconIv.setImageResource(R.drawable.img_avatar_default);
			setText("", mNameTv);
			setText("", mNameEt);
			setText("", mBirthTv);
			setText("", mSexTv);
		}
	}

	private void setText(String string, TextView textView) {
		if (TextUtils.isEmpty(string)) {
			string = "";
		}
		textView.setText(string);
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		if(isModifyMode){
			switchMode();
			return;
		}
		super.onBackPressed();
	}
	
	private void switchMode(){
		isModifyMode=!isModifyMode;
		if(isModifyMode){
			mWeightTv.setVisibility(View.GONE);
			mWeightEt.setVisibility(View.VISIBLE);
			mHeightTv.setVisibility(View.GONE);
			mHeightEt.setVisibility(View.VISIBLE);
			mNameTv.setVisibility(View.GONE);
			mNameEt.setVisibility(View.VISIBLE);
			mNameEt.requestFocus();
			mNameEt.setSelection(mNameEt.getText().length());
            tvUploadAvatar.setVisibility(View.VISIBLE);
			setRightTvText("完成");
			AppUtil.showSoftInputMethod(this, mNameEt);
		}else {
			mWeightTv.setVisibility(View.VISIBLE);
			mWeightEt.setVisibility(View.GONE);
			mHeightTv.setVisibility(View.VISIBLE);
			mHeightEt.setVisibility(View.GONE);
			mNameTv.setVisibility(View.VISIBLE);
			mNameEt.setVisibility(View.GONE);
            tvUploadAvatar.setVisibility(View.INVISIBLE);
            setRightTvText("修改");
			AppUtil.hideSoftInputMethod(this, mNameEt);
		}
		
		mIconIv.setEnabled(isModifyMode);
		mBirthTv.setEnabled(isModifyMode);
		mSexTv.setEnabled(isModifyMode);
	}
	
	
	@Override
	public void onRightClick() {
		// TODO Auto-generated method stub
		if(isModifyMode){
			updateInfo();
		}else {
			switchMode();
		}
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(v==mBirthTv){
			if(mDialog!=null&&!mDialog.isShowing()){
				mDialog.show();
			}
		}else if (v==mIconIv) {
			showPhotoWindow();
		}else if (v==mSexTv) {
			showSexWindow();
		}else {
			super.onClick(v);
		}
	}


	private void showPhotoWindow() {
		new IOSDialogUtil(this).builder().setCancelable(true).setCanceledOnTouchOutside(true)
				.addSheetItem("拍照", SheetItemColor.Black, new OnSheetItemClickListener() {
					@Override
					public void onClick(int which) {
						File file = new File(ImageUtil.filePath);
						if (!file.exists()) {
							file.mkdirs();
						}
						Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						intent1.putExtra(MediaStore.EXTRA_OUTPUT,
								Uri.fromFile(new File(ImageUtil.filePath, "123.PNG")));
						startActivityForResult(intent1, TAKE_PHOTO);
					}
				}).addSheetItem("本地获取", SheetItemColor.Black, new OnSheetItemClickListener() {
					@Override
					public void onClick(int which) {
						Intent intent2 = new Intent(Intent.ACTION_PICK,
								MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						intent2.setType("image/*");
						startActivityForResult(intent2, TAKE_PICTURE);
					}
				}).show();
	}
	
	

	private void showSexWindow() {
		new IOSDialogUtil(this).builder().setCancelable(true).setCanceledOnTouchOutside(true)
				.addSheetItem("男", SheetItemColor.Black, new OnSheetItemClickListener() {
					@Override
					public void onClick(int which) {
						mSexTv.setText("男");
						sex="1";
					}
				}).addSheetItem("女", SheetItemColor.Black, new OnSheetItemClickListener() {
					@Override
					public void onClick(int which) {
						mSexTv.setText("女");
						sex="2";
					}
				}).show();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			String sdCardState = Environment.getExternalStorageState();
			if (requestCode == RE_REGION) {
				initData();
			} else if (requestCode == RE_NAME) {

				initData();
			} else if (!sdCardState.equals(Environment.MEDIA_MOUNTED)) {
				return;
			} else if (requestCode == 222) {
				finish();
			} else {

				switch (requestCode) {
				case TAKE_PHOTO:
					mFilePath = ImageUtil.filePath + "123.PNG";
					mFilePath = ImageUtil.bitmap2File(mFilePath, new Date().getTime() + ".PNG");

					File file = new File(mFilePath);
					if (!file.exists()) {
						try {
							file.createNewFile();
						} catch (Exception e) {
						}
					}
					startPhotoZoom(Uri.fromFile(file), 100);

					break;
				case TAKE_CROP:// // 裁剪成功后显示图片

					Bundle bundle = data.getExtras();
					if (bundle != null) {
						Bitmap bitmap = bundle.getParcelable("data");
						if (bitmap != null) {

							avatar = ImageUtil.saveImag(bitmap, new Date().getTime() + ".PNG");
							mIconIv.setImageBitmap(bitmap);
//							putAvatar(bitmap, pFile);
						}

					}
					break;
				case TAKE_PICTURE:

					Uri imgUri_2 = data.getData();

					startPhotoZoom(imgUri_2, 100);
					break;

				}

			}
		}
	}


	private void setUserInfo(UserInfo userInfo) {
		// TODO Auto-generated method stub
		mUserInfo=userInfo;
		setText(userInfo.birthday, mBirthTv);
		setText(userInfo.nick_name, mNameTv);
		setText(userInfo.nick_name, mNameEt);
		setText(userInfo.height, mHeightTv);
		setText(userInfo.height, mHeightEt);
		setText(userInfo.weight, mWeightTv);
		setText(userInfo.weight, mWeightEt);
		if (userInfo.sex.equals("1")) {
			mSexTv.setText("男");
		} else if (userInfo.sex.equals("2")) {
			mSexTv.setText("女");
		} else {
			mSexTv.setText("男");
		}
	}

	/**
	 * 修改信息
	 */
	private void updateInfo() {
		HttpRequester requester = new HttpRequester();
		if (mBirthday != null) {
			requester.mParams.put("birthday", mBirthday);
		}
		if (sex != null) {
			requester.mParams.put("sex", sex);
		}
		String real_name=mNameEt.getText().toString().trim();
		if (real_name .length()>0) {
			requester.mParams.put("nick_name", real_name);
		}else {
			requester.mParams.put("nick_name", "");
			/*AppUtil.showToast(this, "请输入姓名");
			return;*/
		}

		final String weight=mWeightEt.getText().toString().trim();
		if (weight .length()>0) {
			requester.mParams.put("weight", weight);
		}else {
			requester.mParams.put("weight", "");
			/*AppUtil.showToast(this, "请输入姓名");
			return;*/
		}

		final String height=mHeightEt.getText().toString().trim();
		if (height.length()>0) {
			requester.mParams.put("height", height);
		}else {
			requester.mParams.put("height", "");
			/*AppUtil.showToast(this, "请输入姓名");
			return;*/
		}

		if (avatar != null) {
			requester.mParams.put("avatar", avatar);
		}
		DialogUtil.showDialog(lodDialog);
		NetworkWorker.getInstance().post(AppUrls.getInstance().URL_USER_INFO_SAVE, new ICallback() {

			@Override
			public void onResponse(int status, String result) {
                DialogUtil.dismissDialog(lodDialog);
                if(status==200){
					BaseObject<Object> object=GsonParser.getInstance().parseToObj(result, Object.class);
					if(object!=null){
						if(object.data!=null&&object.status==BaseObject.STATUS_OK){

                            loadPageInfo(true);
							//设置手环基本信息
							if(!AppUtil.isNull(PreferencesUtils.getString(WatchService.FLAG_CURRENT_DEVICE_ADDRESS))){
								Intent intent=new Intent(WatchConstant.ACTION_WATCH_CMD_SET_INFO);
								intent.putExtra(WatchConstant.FLAG_USER_INFO,new byte[]{sex.equals("1")?(byte)1:(byte)0,(byte)DateTimeUtils.getIntervalDays(mBirthday,new Date()),Integer.valueOf(height).byteValue(),Integer.valueOf(weight).byteValue()});
								sendBroadcast(intent);
							}
						}else {
							AppUtil.showToast(getApplicationContext(), object.info);
						}
					}else {
						AppUtil.showToast(getApplicationContext(), "修改失败");
					}
				}else {
					AppUtil.showToast(getApplicationContext(), "请检查网络");
				}

			}

		}, requester);

	}


	/**
	 * 跳转至系统截图界面进行截图
	 * 
	 * @param data
	 * @param size
	 */
	private void startPhotoZoom(Uri data, int size) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(data, "image/*");
		// crop为true时表示显示的view可以剪裁
		intent.putExtra("crop", "true");
		// aspectX aspectY 是宽高的比例
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);
		// outputX,outputY 是剪裁图片的宽高
		intent.putExtra("outputX", size);
		intent.putExtra("outputY", size);
		intent.putExtra("return-data", true);
		startActivityForResult(intent, TAKE_CROP);
	}

	/*
	 * dataPick滑动 scrollListener
	 */
	OnWheelScrollListener scrollListener = new OnWheelScrollListener() {

		@Override
		public void onScrollingStarted(WheelView wheel) {

		}

		@Override
		public void onScrollingFinished(WheelView wheel) {
			checkLimit(wheel);
			/*int n_year = year.getCurrentItem() + 1900;// 年
			int n_month = month.getCurrentItem() + 1;// 月
			initDay(n_year, n_month);
			mBirthday = new StringBuilder().append((year.getCurrentItem() + 1900)).append("-")
					.append((month.getCurrentItem() + 1) < 10 ? "0" + (month.getCurrentItem() + 1)
							: (month.getCurrentItem() + 1))
					.append("-").append(((day.getCurrentItem() + 1) < 10) ? "0" + (day.getCurrentItem() + 1)
							: (day.getCurrentItem() + 1))
					.toString();*/
		}
	};

	private void checkLimit(WheelView wheel){
		int monthInt=month.getCurrentItem()+1;
		int dayInt=day.getCurrentItem()+1;
		String selectDate=(year.getCurrentItem()+1900)+"-"+(monthInt<10?"0"+monthInt:monthInt)+"-"+(dayInt<10?"0"+dayInt:dayInt)+" 00:00:00";
		LogUtil.d("wheel checkLimit---"+wheel+"---selectDate="+selectDate);
		if(DateUtil.beforeNow(selectDate)){
			int n_year = year.getCurrentItem() + 1900;// 年
			int n_month = month.getCurrentItem() + 1;// 月
			initDay(n_year, n_month);
			mBirthday = new StringBuilder().append((year.getCurrentItem() + 1900)).append("-")
					.append((month.getCurrentItem() + 1) < 10 ? "0" + (month.getCurrentItem() + 1)
							: (month.getCurrentItem() + 1))
					.append("-").append(((day.getCurrentItem() + 1) < 10) ? "0" + (day.getCurrentItem() + 1)
							: (day.getCurrentItem() + 1))
					.toString();
			return;
		}
		month.setCurrentItem(norMonth-1,true);
		day.setCurrentItem(norDay-1,true);
		int n_year = norYear ;// 年
		int n_month = norMonth ;// 月
		initDay(n_year, n_month);
		mBirthday = new StringBuilder().append((norYear )).append("-")
				.append((norMonth ) < 10 ? "0" + (norMonth ) : (norMonth ))
				.append("-").append(( norDay < 10) ? "0" + (norDay ) : (norDay ))
				.toString();
	}

	private void initDay(int arg1, int arg2) {
		// 设置天数
		NumericWheelAdapter numericWheelAdapter = new NumericWheelAdapter(this, 1, getDay(arg1, arg2), "%02d");
		numericWheelAdapter.setLabel("日");
		day.setViewAdapter(numericWheelAdapter);
	}

	/**
	 * 
	 * @param year
	 * @param month
	 * @return int
	 * @author lilifeng
	 */
	private int getDay(int year, int month) {
		int day = 31;
		boolean flag = false;
		switch (year % 4) {
		case 0:
			flag = true;
			break;
		default:
			flag = false;
			break;
		}
		switch (month) {
		case 4:
			day = 30;
			break;
		case 6:
			day = 30;
			break;
		case 9:
			day = 30;
			break;
		case 11:
			day = 30;
			break;
		case 2:
			day = flag ? 29 : 28;
			break;
		default:
			day = 31;
			break;
		}
		return day;
	}

	int norYear=0;
	int norMonth=0;
	int norDay=0;
	/**
	 * 时间选择控价
	 * 
	 * @return
	 */
	private View getDataPick() {
		Calendar c = Calendar.getInstance();
		c.setTimeZone(TimeZone.getTimeZone("GMT+8"));
		c.setTimeInMillis(System.currentTimeMillis());
		norYear = c.get(Calendar.YEAR);
		norMonth = c.get(Calendar.MONTH)+1;
		norDay = c.get(Calendar.DAY_OF_MONTH);
		int curYear = mYear;
		int curMonth = mMonth + 1;
		int curDate = mDay;

		view = LayoutInflater.from(this).inflate(R.layout.dialog_wheel, null);
		view.findViewById(R.id.wheel_okTv).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mBirthTv.setText(mBirthday);
				if (mDialog != null && mDialog.isShowing()) {
					mDialog.dismiss();
				}
			}
		});
		year = (WheelView) view.findViewById(R.id.wheel_yearWv);
		/**
		 * 设置年份
		 */
		NumericWheelAdapter numericWheelAdapter1 = new NumericWheelAdapter(this, 1900, norYear);
		numericWheelAdapter1.setLabel("年");
		year.setViewAdapter(numericWheelAdapter1);
		year.setCyclic(true);// 是否可循环滑动
		year.addScrollingListener(scrollListener);

		month = (WheelView) view.findViewById(R.id.wheel_monthWv);
		/**
		 * 设置月份
		 */
		NumericWheelAdapter numericWheelAdapter2 = new NumericWheelAdapter(this, 1, 12, "%02d");
		numericWheelAdapter2.setLabel("月");
		month.setViewAdapter(numericWheelAdapter2);
		month.setCyclic(true);
		month.addScrollingListener(scrollListener);

		day = (WheelView) view.findViewById(R.id.wheel_dayWv);
		initDay(curYear, curMonth);
		day.addScrollingListener(scrollListener);
		day.setCyclic(true);

		year.setVisibleItems(9);// 设置显示行数
		month.setVisibleItems(9);
		day.setVisibleItems(9);

		year.setCurrentItem(curYear - 1900);
		month.setCurrentItem(curMonth - 1);
		day.setCurrentItem(curDate - 1);

		return view;
	}
	
	public static void invoke(Context context){
		Intent intent =new Intent(context,MyInfoActivity.class);
		context.startActivity(intent);
	}

}
