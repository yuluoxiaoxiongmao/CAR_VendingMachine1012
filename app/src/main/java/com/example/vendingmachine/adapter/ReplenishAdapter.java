package com.example.vendingmachine.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.vendingmachine.App;
import com.example.vendingmachine.R;
import com.example.vendingmachine.bean.GoodsInfoBean;
import com.example.vendingmachine.platform.http.HttpUrl;
import com.example.vendingmachine.utils.GlideCircleTransform;

import java.util.HashMap;
import java.util.List;

/**
 * HE 2018-09-20.
 */

public class ReplenishAdapter extends RecyclerView.Adapter<ReplenishAdapter.ViewHolder>{

    private static List<GoodsInfoBean.MessageBean> list;
    private Context context;
    private ButtonInterface buttonInterface;
    public static HashMap<Integer, Integer> isSelected;

    public ReplenishAdapter(List<GoodsInfoBean.MessageBean> list,Context context) {
        this.context = context;
        this.list = list;
        init();
    }

    private static void init() {
        isSelected = new HashMap<Integer, Integer>();
        for (int i = 0; i < list.size(); i++) {
            isSelected.put(i, 1);
        }
    }
    /**
     *按钮点击事件需要的方法
     */
    public void buttonSetOnclick(ButtonInterface buttonInterface){
        this.buttonInterface=buttonInterface;
    }
    /**
     * 按钮点击事件对应的接口
     */
    public interface ButtonInterface{
        void onclick(View view, int position, int id);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(App.Companion.getMContext()).inflate(R.layout.replenish_list_itm,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (holder==null){return;}
        GoodsInfoBean.MessageBean goodsUser = list.get(position);
        holder.tv_cargo_lane_text.setText(goodsUser.getBox());
        holder.tv_stock_text.setText(goodsUser.getStore()+"");
        Glide.with(App.Companion.getMContext()).load(HttpUrl.SERVER_URL+goodsUser.getPic_url())
                .placeholder(R.mipmap.iv_goods_img_no).transform(new GlideCircleTransform(context)).into(holder.iv_bh_sp_img);
        if (isSelected!=null){
            switch (isSelected.get(position)){
                case 1:
                    holder.tv_testing_electric.setTextColor(Color.parseColor("#ffffff"));
                    holder.tv_testing_electric.setText("未测试");
                    break;
                case 2:
                    holder.tv_testing_electric.setTextColor(Color.parseColor("#FF4081"));
                    holder.tv_testing_electric.setText("测试中..");
                    break;
                case 3:
                    holder.tv_testing_electric.setTextColor(Color.parseColor("#FF4081"));
                    holder.tv_testing_electric.setText("Success");
                    break;
                case 4:
                    holder.tv_testing_electric.setTextColor(Color.parseColor("#FF4081"));
                    holder.tv_testing_electric.setText("Failure");
                    break;
            }
        }
        holder.bt_re_testing_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonInterface!=null){
                    buttonInterface.onclick(view,position,0);
                }
                setPositions(position,2);
            }
        });
        holder.bt_plus_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonInterface!=null){
                    buttonInterface.onclick(view,position,1);
                }
            }
        });
        holder.bt_reduce_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonInterface!=null){
                    buttonInterface.onclick(view,position,2);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list==null?0:list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView tv_goods_id_text;
        TextView tv_cargo_lane_text;
        TextView tv_stock_text;
        TextView tv_testing_electric;
        ImageView iv_bh_sp_img;
        Button bt_plus_button;
        Button bt_re_testing_button;
        Button bt_reduce_button;

        public ViewHolder(View itemView) {
            super(itemView);
            tv_cargo_lane_text = itemView.findViewById(R.id.tv_cargo_lane_text);
            tv_goods_id_text = itemView.findViewById(R.id.tv_goods_id_text);
            tv_stock_text = itemView.findViewById(R.id.tv_stock_text);
            bt_re_testing_button = itemView.findViewById(R.id.bt_re_testing_button);
            bt_plus_button = itemView.findViewById(R.id.bt_plus_button);
            bt_reduce_button = itemView.findViewById(R.id.bt_reduce_button);
            tv_testing_electric = itemView.findViewById(R.id.tv_testing_electric);
            iv_bh_sp_img = itemView.findViewById(R.id.iv_bh_sp_img);
        }
    }

    public void setPositions(int ps,int msg){
        isSelected.put(ps,msg);
        notifyDataSetChanged();
    }
}
