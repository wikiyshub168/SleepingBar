package com.bolaa.sleepingbar.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;

import com.bolaa.sleepingbar.R;
import com.bolaa.sleepingbar.controller.AbstractListAdapter;
import com.bolaa.sleepingbar.utils.Image13Loader;
import com.core.framework.app.devInfo.ScreenUtil;


public class PictureAdapter extends AbstractListAdapter<String> {

	private int itemHeight;
	private String img_path;

	public PictureAdapter(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public void setWidth(int width,int column){
		itemHeight=(width-ScreenUtil.dip2px(mContext,5)*(column-1))/column;
	}

	public void setImagePath(String img_path){
		this.img_path=img_path;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		// TODO Auto-generated method stub
		ViewHolder holder=null;
		if(view==null){
            view=View.inflate(mContext, R.layout.item_picture, null);
			holder=new ViewHolder(view);
			view.setTag(holder);
		}else {
			holder=(ViewHolder)view.getTag();
		}

		Image13Loader.getInstance().loadImageFade(img_path+mList.get(i),holder.ivPic);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

		return view;
	}

	class ViewHolder{
		public ImageView ivPic;
		public ViewHolder(View view){
            ivPic=(ImageView) view.findViewById(R.id.iv_pic);
            ViewGroup.LayoutParams lp=ivPic.getLayoutParams();
            lp.height=itemHeight;
            ivPic.setLayoutParams(lp);
		}
		
	}

}
