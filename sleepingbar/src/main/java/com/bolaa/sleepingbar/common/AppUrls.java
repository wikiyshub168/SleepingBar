package com.bolaa.sleepingbar.common;

public class AppUrls {
	private static AppUrls mUrls;

	public static AppUrls getInstance() {
		if (mUrls == null) {
			synchronized (AppUrls.class) {
				if (mUrls == null) {
					mUrls = new AppUrls();
				}
			}
		}
		return mUrls;
	}
	public String app_token = "6HDm4jAUv4w5W-ycjAvZFo630qHrXJS60yNKT1r";// 测试拟定的手机标识码

//	public String HYH_URL = "http://haiyuehuitest.m1ju.com/api.php?";// 测试地址
	public String HYH_URL = "http://test.haiyuehui.com/api.php?";// 正式环境的测试服务器


	public String URL_THIRD_LOGIN_QQ = HYH_URL + "method=customer/customer_login_by_qq";//尝试qq三方登录
	public String URL_THIRD_LOGIN_WX = HYH_URL + "method=customer/login_by_weichat";//尝试微信三方登录
	
	public String URL_THIRD_ACCOUNT_UNBIND = HYH_URL + "method=customer/set_account_unbind";//解除绑定
	
	//------------------------ 华丽的分割线----------------
	public String APP_KEY="test";
	public String BASE_URL="http://192.168.2.126/beijingsleep/app/";
	
	public String URL_REGISTER=BASE_URL+"&c=user&act=reg_act";
	public String URL_USER_INFO_SAVE=BASE_URL+"&c=user&act=edit_profile";//修改或保存个人资料

	public String URL_HOSPITAL_MAKE_AN_APPOINTMENT=BASE_URL+"&c=hospital&act=appointment";//预约
	public String URL_REGION_ALL=BASE_URL+"&c=index&act=region_list";//全部地区

	public String URL_CASH_HISTORY=BASE_URL+"&c=account&act=cash_log";//提现记录

	public String URL_FIND_PWD_VERIFY_CAPTCHA=BASE_URL+"&c=user&act=get_password";//找回密码--校验验证码
	public String URL_FIND_PWD=BASE_URL+"&c=user&act=edit_password";//找回密码
	public String URL_COUNPONS_LIST=BASE_URL+"&c=bonus&act=mybonus";//优惠券 is_used  0可使用1过期2已使用3未到使用
	public String URL_PAY_PAGE_INFO=BASE_URL+"&c=hospital&act=checkout";//结算页面的信息
	public String URL_ORDER_PAY_PAGE_INFO=BASE_URL+"&c=order&act=pay";//订单去支付页面信息(订单详情)
	public String URL_DO_PAY=BASE_URL+"&c=order&act=dopay";//提交支付方式，返回支付单
	public String URL_HOSPITAL_EVALUATE_COMMIT=BASE_URL+"&c=order&act=save_comment";//机构评价
	public String URL_HOSPITAL_COMBO_LIST=BASE_URL+"&c=hospital&act=package_list";//体检套餐列表
	public String URL_HOSPITAL_EVALUATE_LIST=BASE_URL+"&c=hospital&act=comment_list";//评论列表
	public String URL_CHECK_UPDATE=BASE_URL+"";//升级

	public String URL_SUPPORTER_LIST=BASE_URL+"sleep.php?act=my_funds";//我的基金页面，也就是基金支持者列表
	public String URL_SUPPORTER_DETAIL=BASE_URL+"sleep.php?act=fund_detail";//某支持者对我支持基金的明细
	public String URL_FUNDS_RANKING_LIST=BASE_URL+"sleep.php?act=friend_fund_ranking";//基金排行
	public String URL_MEDAL_LIST=BASE_URL+"user.php?act=my_medal";//我的勋章
	public String URL_GET_CAPTCHA=BASE_URL+"user.php?act=send_verify_code";
	public String URL_LOGIN=BASE_URL+"user.php?act=login";//登录
	public String URL_LOGOUT=BASE_URL+"user.php?act=logout";//退出登录
	public String URL_DEAL_HISTORY=BASE_URL+"user.php?act=account_log";//历史交易记录
	public String URL_ACCOUNT_INFO=BASE_URL+"user.php?act=user_account";//个人中心我的账户
	public String URL_CASH_WITHDRAW=BASE_URL+"user.php?act=do_apply_cash";//提现
	public String URL_CASH_WITHDRAW_PAGE_INFO=BASE_URL+"user.php?act=apply_cash";//提现页面信息
	public String URL_DO_RECHARGE=BASE_URL+"user.php?act=do_recharge";//充值
	public String URL_DEVICE_HISTORY=BASE_URL+"user.php?act=equipment_list";//已经绑定的设备列表
	public String URL_MY_FRIENDS_LIST=BASE_URL+"community.php?act=my_friend";//好友
	public String URL_SEARCH_FRIENDS=BASE_URL+"community.php?act=search_index";//添加好友页面
	public String URL_DO_CARE=BASE_URL+"community.php?act=do_care";//关注
	public String URL_CANCEL_CARE=BASE_URL+"community.php?act=cancel_care";//取消关注
	public String URL_TOPIC_LIST=BASE_URL+"community.php?act=topic_list";//社区首页的话题
	public String URL_COMMUNITY_INFORMATION=BASE_URL+"community.php?act=index";//社区首页的图文和文字咨询

	public String URL_ARTICAL_SERVICE_PROTOCAL="http://baidu.com";
	public String URL_ARTICAL_LAW="http://baidu.com";
	public String URL_ARTICAL_ABOUT="http://baidu.com";


	/*支付宝回调地址*/
	public String URL_ZFB_NOTIFY="http://jk.m1ju.com/app/respond_alipay.php";




}
