package com.bolaa.sleepingbar.adapter;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bolaa.sleepingbar.R;
import com.bolaa.sleepingbar.common.APIUtil;
import com.bolaa.sleepingbar.common.AppUrls;
import com.bolaa.sleepingbar.controller.AbstractListAdapter;
import com.bolaa.sleepingbar.httputil.ParamBuilder;
import com.bolaa.sleepingbar.model.Medal;
import com.bolaa.sleepingbar.model.Supporter;
import com.bolaa.sleepingbar.ui.SupporterDetailActivity;
import com.bolaa.sleepingbar.utils.AppUtil;
import com.bolaa.sleepingbar.utils.Image13Loader;
import com.bolaa.sleepingbar.utils.ShareUtil;
import com.core.framework.app.devInfo.ScreenUtil;
import com.core.framework.util.DialogUtil;
import com.core.framework.util.MD5Util;

/**
 * 勋章
 */
public class MedalAdapter extends AbstractListAdapter<Medal> {
	private Dialog medalDialog;

	public MedalAdapter(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		// TODO Auto-generated method stub
		ViewHolder holder=null;
		if(view==null){
			view=View.inflate(mContext, R.layout.item_medal, null);
			holder=new ViewHolder(view);
			view.setTag(holder);
		}else {
			holder=(ViewHolder)view.getTag();
		}
		final Medal medal=mList.get(i);
		holder.tvName.setText(medal.m_name);
		holder.tvDate.setText(medal.c_time);
		Image13Loader.getInstance().loadImageFade(medal.img,holder.ivAvatar);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showMedail(medal);
			}
		});
		return view;
	}
	
	class ViewHolder{
		public TextView tvName;
		public TextView tvDate;
		public ImageView ivAvatar;
		
		public ViewHolder(View view){
			tvName=(TextView)view.findViewById(R.id.tv_name);
			tvDate=(TextView)view.findViewById(R.id.tv_date);
			ivAvatar=(ImageView) view.findViewById(R.id.iv_avatar);
			ViewGroup.LayoutParams lp=ivAvatar.getLayoutParams();
			lp.height= (ScreenUtil.WIDTH - ScreenUtil.dip2px(mContext,4*20))/3;
			ivAvatar.setLayoutParams(lp);
		}
		
	}

	public TextView tvName;
	public TextView tvDate;
	public TextView tvDetail;
	public TextView btnShare;
	public ImageView ivAvatar;
	public ImageView ivCancel;
	private void showMedail(final Medal medal){
		if(medalDialog==null){
			View dialogView=LayoutInflater.from(mContext).inflate(R.layout.dialog_medal, null);
			medalDialog = DialogUtil.getCenterDialog((Activity) mContext,dialogView );
			tvName=(TextView) dialogView.findViewById(R.id.tv_name);
			tvDate=(TextView)medalDialog.findViewById(R.id.tv_date);
			tvDetail=(TextView)medalDialog.findViewById(R.id.tv_detail);
			btnShare=(TextView)medalDialog.findViewById(R.id.btn_share);
			ivAvatar=(ImageView) medalDialog.findViewById(R.id.iv_avatar);
			ivCancel=(ImageView) medalDialog.findViewById(R.id.iv_cancel);
			btnShare.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
                    ParamBuilder params=new ParamBuilder();
                    params.clear();
                    params.append("id",medal.id);
                    params.append("s",MD5Util.getMD5(medal.id+"_iphone_anandroid"));
                    new ShareUtil((Activity) mContext,medal.m_name, medal.m_explain, APIUtil.parseGetUrlHasMethod(params.getParamList(),AppUrls.getInstance().URL_MEDAL_SHARE),medal.img).showShareDialog();
				}
			});
			ivCancel.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(!((Activity)mContext).isFinishing())DialogUtil.dismissDialog(medalDialog);
				}
			});
		}
		tvName.setText(medal.m_name);
		tvDate.setText("获得时间："+medal.c_time);
		tvDetail.setText(medal.m_explain);
		Image13Loader.getInstance().loadImageFade(medal.img,ivAvatar);
		DialogUtil.showDialog(medalDialog);
	}

}
