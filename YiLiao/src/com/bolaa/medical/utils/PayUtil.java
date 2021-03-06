package com.bolaa.medical.utils;

import java.net.URLEncoder;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;
import com.bolaa.medical.common.AppStatic;
import com.bolaa.medical.common.AppUrls;
import com.bolaa.medical.model.WXInfo;
import com.tencent.mm.sdk.constants.Build;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.unionpay.UPPayAssistEx;

public class PayUtil {
	private static final int SDK_PAY_FLAG = 1;
	private static final int SDK_CHECK_FLAG = 2;
	// 商户PID
	public static final String PARTNER = "2088611037557465";
	// 商户收款账号
	public static final String SELLER = "dcst@dcstgroup.com";
	// 商户私钥，pkcs8格式
	public static final String RSA_PRIVATE = "MIICXQIBAAKBgQC89MbJHMa6H8f+VVtb28M8DWnj5M4mG079rx9/h0DBDR+d1MbDKs6EicmHHF5gwwLnxoIpk1j46mnF6n4BcBDfocnQj9zwIifGsjsR+JXzbMqf1cMZGHwyujBxYZr9D/J+WRzZHgbWZx1BP9XmCE5QkJZ/0KIOuvybvgBtA/SwCwIDAQABAoGBAJHkiKt1PXct3LPh4cUd/DMcxDqCSi0f/rBei3piyruDz3qEc+by4TtyS5i3baNWTqL4IT3Kl/Kww3RdpmajyVIdDmVq1PRMOD4frYba/dXqdQER5vD7ldg5HS5qn+MlH6fSIrvCUWCfL3ejUajsk5GWH9NG3h3v+eYcaVDTl8kZAkEA7pGyHINZlFaRLELUrpfkbueHP30XYp8DkQjNJoV4WTdiMcOuPer8l6VtiC1hurKCXBMPLxReO7EaBNUbts1FHwJBAMrDGPccDXL3ItXHuBaNWbWcp3/bLCEInkqF8S6Z+/vudgVokXSMvJ8Bcq2ejInPeq3ycle2bpxoBmJADIb465UCQDZaJT0Pw9Hi4xI1a6UXX+jQgOS7CB/k4HgjjDGxiNiyoIF79m+O4NtfyhOTW0egschuYzAzsMBiue3N65F7NLsCQEAdLuC8cxg+QzqcG36uFYbS0TghorOTWRIxhlD5Ce/guFr/dLcI5X/V4mA5+TB+dclZF4Tav+EfF52rqQpo3X0CQQCv6+XE3LM/YdNmU7fGVufg7EjIxkqin1PoFtb609wkKXuK689Vbpd2IPDcmldzZBjGBa+YoRxg5wvgB+Ey+B/p";



	public static String union_Model = "00";// 00-正式 01-测试(银联)

	private static PayListener mPayListener;

	private static Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SDK_PAY_FLAG: {
				PayResult payResult = new PayResult((String) msg.obj);

				// 支付宝返回此次支付结果及加签，建议对支付宝签名信息拿签约时支付宝提供的公钥做验签
				String resultInfo = payResult.getResult();

				String resultStatus = payResult.getResultStatus();

				// 判断resultStatus 为“9000”则代表支付成功，具体状态码代表含义可参考接口文档
				if (TextUtils.equals(resultStatus, "9000")) {
					mPayListener.resultForZhifubao(1, "支付成功");
				} else {
					// 判断resultStatus 为非“9000”则代表可能支付失败
					// “8000”代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
					if (TextUtils.equals(resultStatus, "8000")) {
						mPayListener.resultForZhifubao(0, "支付结果确认中,请耐心等待");

					} else {
						mPayListener.resultForZhifubao(0, "支付失败");
					}
				}
				break;
			}
			case SDK_CHECK_FLAG: {
				mPayListener.resultForZhifubao(0, (String) msg.obj);
				break;
			}
			default:
				break;
			}
		};
	};

	/**
	 * 银联支付
	 * 
	 * @param activity
	 * @param id
	 *            交易订单号，由银联生成
	 */
	public static void wayToYinlian(Activity activity, String id) {
		UPPayAssistEx.startPay(activity, null, null, id, union_Model);
	}

	/**
	 * 微信支付
	 * 
	 * 
	 * @param context
	 * @param appId
	 *            公众账号ID
	 * @param partnerId
	 *            商户号
	 * @param prepayId
	 *            预支付交易会话ID
	 * @param packageValue
	 *            扩展字段 暂填写固定值Sign=WXPay
	 * @param nonceStr
	 *            随机字符串，不长于32位。推荐随机数生成算法
	 * @param timeStampe
	 *            时间戳
	 * @param sign
	 *            签名
	 */
	public static void wayToWX(Context context, WXInfo wxInfo) {
		IWXAPI msgApi = WXAPIFactory.createWXAPI(context, null);
		msgApi.registerApp(AppStatic.WX_APPID);

		boolean isPaySupported = msgApi.getWXAppSupportAPI() >= Build.PAY_SUPPORTED_SDK_INT;
		if (!isPaySupported) {
			Toast.makeText(context, "请先打开微信", Toast.LENGTH_SHORT).show();
			return;
		}

		PayReq request = new PayReq();
		request.appId = wxInfo.getAppid();
		request.partnerId = wxInfo.getMch_id();
		request.prepayId = wxInfo.getPrepay_id();
		request.nonceStr = wxInfo.getNonce_str();
		request.timeStamp = wxInfo.getTimestamp();
		request.packageValue = "Sign=WXPay";
		request.sign = wxInfo.getSign();
		msgApi.sendReq(request);
	}

	/**
	 * 支付宝支付
	 */
	public static void wayToZhifubao(final Context context, String price,
			String pay_sn) {
		mPayListener = (PayListener) context;
		// 订单
		String orderInfo = getOrderInfo(price, pay_sn);

		try {
			// 对订单做RSA 签名
			String sign = SignUtils.sign(orderInfo, RSA_PRIVATE);
			// 仅需对sign 做URL编码
			sign = URLEncoder.encode(sign, "UTF-8");
			// 完整的符合支付宝参数规范的订单信息
			final String payInfo = orderInfo + "&sign=\"" + sign + "\"&"
					+ "sign_type=\"RSA\"";

			Runnable payRunnable = new Runnable() {

				@Override
				public void run() {
					// 构造PayTask 对象
					PayTask alipay = new PayTask((Activity) context);
					// 调用支付接口，获取支付结果
					String result = alipay.pay(payInfo);

					Message msg = new Message();
					msg.what = SDK_PAY_FLAG;
					msg.obj = result;
					mHandler.sendMessage(msg);
				}
			};

			// 必须异步调用
			Thread payThread = new Thread(payRunnable);
			payThread.start();
		} catch (Exception e) {
		}
	}

	/**
	 * create the order info. 创建订单信息
	 * 
	 */
	private static String getOrderInfo(String price, String pay_sn) {

		// 签约合作者身份ID
		String orderInfo = "partner=" + "\"" + PARTNER + "\"";

		// 签约卖家支付宝账号
		orderInfo += "&seller_id=" + "\"" + SELLER + "\"";

		// 商户网站唯一订单号
		orderInfo += "&out_trade_no=" + "\"" + pay_sn + "\"";

		// 商品名称
		orderInfo += "&subject=" + "\"" + "维极体检套餐购买_" + pay_sn + "\"";

		// 商品详情
		orderInfo += "&body=" + "\"" + pay_sn + "\"";

		// 商品金额
		orderInfo += "&total_fee=" + "\"" + price + "\"";

		// 服务器异步通知页面路径
		orderInfo += "&notify_url=" + "\"" + AppUrls.getInstance().URL_ZFB_NOTIFY +"\"";

		// 服务接口名称， 固定值
		orderInfo += "&service=\"mobile.securitypay.pay\"";

		// 支付类型， 固定值
		orderInfo += "&payment_type=\"1\"";

		// 参数编码， 固定值
		orderInfo += "&_input_charset=\"utf-8\"";

		// 设置未付款交易的超时时间
		// 默认30分钟，一旦超时，该笔交易就会自动被关闭。
		// 取值范围：1m～15d。
		// m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
		// 该参数数值不接受小数点，如1.5h，可转换为90m。
		orderInfo += "&it_b_pay=\"30m\"";

		// extern_token为经过快登授权获取到的alipay_open_id,带上此参数用户将使用授权的账户进行支付
		// orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

		// 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
		orderInfo += "&return_url=\"m.alipay.com\"";
		// orderInfo += "&extra_common_param=\"product_buy\"";

		// 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
		// orderInfo += "&paymethod=\"expressGateway\"";
		return orderInfo;
	}

	public interface PayListener {
		/**
		 * 支付宝返回结果
		 * 
		 * @param state
		 *            0 失败 1 成功
		 * @param pay_sn
		 */
		public void resultForZhifubao(int state, String detail);
	}
}
