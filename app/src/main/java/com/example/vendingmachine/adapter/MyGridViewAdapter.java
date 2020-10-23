package com.example.vendingmachine.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.vendingmachine.App;
import com.example.vendingmachine.R;
import com.example.vendingmachine.db.GoodsUser;
import com.example.vendingmachine.platform.http.HttpUrl;
import com.example.vendingmachine.utils.GlideRoundCornersTransUtils;

import java.util.List;

/**
 *  2018-09-12.
 */

public class MyGridViewAdapter extends BaseAdapter {
    private static List<GoodsUser> listData;
    private LayoutInflater inflater;
    private Context context;
    private int mIndex;//页数下标，表示第几页，从0开始
    private int mPagerSize;//每页显示的最大数量

    public MyGridViewAdapter(Context context, List<GoodsUser> listData, int mIndex, int mPagerSize) {
        this.context = context;
        this.listData = listData;
        this.mIndex = mIndex;
        this.mPagerSize = mPagerSize;
        inflater = LayoutInflater.from(context);

    }

    private ButtonInterface buttonInterface;
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
        void onclick(int position, int id);
    }

    @Override
    public int getCount() {
        return listData.size() > (mIndex + 1)*mPagerSize ? mPagerSize : (listData.size() - mIndex*mPagerSize);
    }

    @Override
    public Object getItem(int position) {
        return listData.get(position + mIndex * mPagerSize);
    }

    @Override
    public long getItemId(int position) {
        return position + mIndex * mPagerSize;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if(convertView == null) {
            convertView = inflater.inflate(R.layout.gridview_list_itm_6,parent,false);
            holder = new ViewHolder();
            holder.iv_goods_img = convertView.findViewById(R.id.iv_goods_img);
            holder.tv_describe = convertView.findViewById(R.id.tv_describe);
            holder.tv_price = convertView.findViewById(R.id.tv_price);
            holder.relative_layout_itm = convertView.findViewById(R.id.relative_layout_itm);
           // holder.bt_at_once_buy = convertView.findViewById(R.id.bt_at_once_buy);
            holder.tv_cargo_lane_itm = convertView.findViewById(R.id.tv_cargo_lane_itm);

            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        //重新确定position（因为拿到的是总的数据源，数据源是分页加载到每页的GridView上的，为了/确保能正确的点对不同页上的item）
        final int pos = position + mIndex*mPagerSize;//假设mPagerSize=8，假如点击的是第二页（即mIndex=1）上的第二个位置item(position=1),那么这个item的实际位置就是pos=9

        holder.relative_layout_itm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setAnimation(AnimationUtils.loadAnimation(context,R.anim.sp_itm_anim));
                if (buttonInterface!=null){
                    buttonInterface.onclick(pos,1);
                }
                notifyDataSetChanged();
            }
        });

        /*holder.bt_at_once_buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (buttonInterface!=null){
                    buttonInterface.onclick(pos,2);
                }
            }
        });*/

        GoodsUser list = listData.get(pos);
//        Glide.with(App.Companion.getMContext()).load(HttpUrl.SERVER_URL+list.getPic_url()).placeholder(R.mipmap.iv_goods_img_no)
//                .bitmapTransform(new GlideRoundCornersTransUtils(context,20,GlideRoundCornersTransUtils.CornerType.TOP)).into(holder.iv_goods_img);
        if(list.getStore() <=0){
            holder.iv_goods_img.setImageResource(R.drawable.img_goods_no);
        }else {
            Glide.with(App.Companion.getMContext()).load(list.getPic_url()).placeholder(R.mipmap.iv_goods_img_no)
                    .bitmapTransform(new GlideRoundCornersTransUtils(context,20,GlideRoundCornersTransUtils.CornerType.TOP)).into(holder.iv_goods_img);
        }
        if(list.getName().contains("充值")){
            holder.relative_layout_itm.setVisibility(View.GONE);
        }
        holder.tv_describe.setText(list.getName());
        holder.tv_price.setText("￥"+list.getPrice());
        holder.tv_cargo_lane_itm.setText(""+list.getBox().replace("A",""));

        return convertView;
    }

    class ViewHolder {
        private ImageView iv_goods_img;
        private LinearLayout relative_layout_itm;
        private TextView tv_describe;
        private TextView tv_price;
        private TextView tv_cargo_lane_itm;
       // private Button bt_at_once_buy;
    }
    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
