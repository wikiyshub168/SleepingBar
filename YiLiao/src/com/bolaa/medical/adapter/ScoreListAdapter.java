package com.bolaa.medical.adapter;

import com.bolaa.medical.R;
import com.bolaa.medical.controller.AbstractListAdapter;
import com.bolaa.medical.model.Hospital;
import com.bolaa.medical.model.Score;
import com.bolaa.medical.ui.HospitalDetailActivity;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ScoreListAdapter extends AbstractListAdapter<Score>{

	public ScoreListAdapter(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		// TODO Auto-generated method stub
		ViewHolder holder=null;
		if(view==null){
			view=View.inflate(mContext, R.layout.item_score, null);
			holder=new ViewHolder(view);
			view.setTag(holder);
		}else {
			holder=(ViewHolder)view.getTag();
		}
		
		final Score score=mList.get(i);
		holder.tvScoreItem.setText(score.change_desc+(score.pay_points>0?" +":" ")+score.pay_points);
		holder.tvScoreTotal.setText(score.change_time);
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		return view;
	}
	
	class ViewHolder{
		public TextView tvScoreItem;
		public TextView tvScoreTotal;
		
		public ViewHolder(View view){
			tvScoreItem=(TextView)view.findViewById(R.id.tv_score_item);
			tvScoreTotal=(TextView)view.findViewById(R.id.tv_score_total);
		}
		
	}

}
