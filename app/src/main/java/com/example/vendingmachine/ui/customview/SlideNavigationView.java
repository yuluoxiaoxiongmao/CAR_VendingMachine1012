package com.example.vendingmachine.ui.customview;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.vendingmachine.R;
import com.example.vendingmachine.adapter.MyGridViewAdapter;
import com.example.vendingmachine.adapter.MyViewPagerAdapter;
import com.example.vendingmachine.db.GoodsUser;
import com.example.vendingmachine.platform.common.Constant;
import com.example.vendingmachine.utils.widget.DepthPageTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * 2018-11-01.
 */

public class SlideNavigationView extends LinearLayout implements MyGridViewAdapter.ButtonInterface {

    private Context context;
    private ImageView[] ivPoints;
    public int totalPage;
    private int index = 0;
    public List<MyGridViewAdapter> adapterlist;
    private List<View> viewPagerList;
    private MyGridViewAdapter adapter;
    private ViewPager my_viewpager;
    private ImageView iv_right_arrow;
    private ImageView iv_left_arrow;
    private LinearLayout points;
    private SlideViewItmInterface anInterface;
    private TextView[] tvPoints;

    private TextView tv_right_arrow;
    private TextView tv_left_arrow;

    public SlideNavigationView(Context context) {
        super(context, null);
    }

    public SlideNavigationView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

    }

    public void SlideViewItmOnclick(SlideViewItmInterface anInterface) {
        this.anInterface = anInterface;
    }

    public interface SlideViewItmInterface {
        void OnItmInterface(int position);
    }

    public void intiData(List<GoodsUser> list) {
//        try {
//            list.remove(list.size()-1);
//        }catch (Exception e){}
        adapterlist = new ArrayList<>();
        totalPage = (int) Math.ceil(list.size() * 1.0 / Constant.mPageSize)-1;
        viewPagerList = new ArrayList<>();
        for (int i = 0; i < totalPage; i++) {
            final GridView gridView = (GridView) View.inflate(context, R.layout.gridview_layout_itm, null);
            adapter = new MyGridViewAdapter(context, list, i, Constant.mPageSize);
            adapterlist.add(adapter);
            gridView.setAdapter(adapter);
            adapter.buttonSetOnclick(this);
            viewPagerList.add(gridView);
        }
        my_viewpager.setAdapter(new MyViewPagerAdapter(viewPagerList));
        my_viewpager.setPageTransformer(true, new DepthPageTransformer());
        points.removeAllViews();
        ivPoints = new ImageView[totalPage];
        tvPoints = new TextView[totalPage];
        for (int i = 0; i < totalPage; i++) {
            ivPoints[i] = new ImageView(context);
//            tvPoints[i] = new TextView(context);
//            tvPoints[i].setText("1");
            if (i == 0) {
                ivPoints[i].setImageResource(R.drawable.dot_yes);
//                tvPoints[i] = new TextView(context);
//                tvPoints[i].setBackground(getResources().getDrawable(R.drawable.shape_cirel_point_yes));
            } else {
                ivPoints[i].setImageResource(R.drawable.dot_no);
//                tvPoints[i] = new TextView(context);
//                tvPoints[i].setBackground(getResources().getDrawable(R.drawable.shape_cirel_point_no));
            }
            ivPoints[i].setPadding(-10, 1, -10, 1);
            points.addView(ivPoints[i]);
//            tvPoints[i].setPadding(-10, 1, -10, 1);
//            points.addView(tvPoints[i]);
        }
        my_viewpager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                index = position;
                for (int i = 0; i < totalPage; i++) {
                    if (i == position) {
                        ivPoints[i].setImageResource(R.drawable.dot_yes);
//                        tvPoints[i].setBackground(getResources().getDrawable(R.drawable.shape_cirel_point_yes));
                    } else {
//                        tvPoints[i].setBackground(getResources().getDrawable(R.drawable.shape_cirel_point_no));
                        ivPoints[i].setImageResource(R.drawable.dot_no);
                    }
                }
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LayoutInflater.from(context).inflate(R.layout.slide_navigation_view, this);
        my_viewpager = findViewById(R.id.my_viewpager);
        iv_left_arrow = findViewById(R.id.iv_left_arrow);
        iv_right_arrow = findViewById(R.id.iv_right_arrow);

        tv_left_arrow = findViewById(R.id.tv_left_arrow);
        tv_right_arrow = findViewById(R.id.tv_right_arrow);

        points = findViewById(R.id.points);

        iv_right_arrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (index < totalPage - 1) {
                    index++;
                    my_viewpager.setCurrentItem(index);
                }
            }
        });
        iv_left_arrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (index > 0) {
                    index--;
                    my_viewpager.setCurrentItem(index);
                }
            }
        });

        tv_right_arrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (index < totalPage - 1) {
                    index++;
                    my_viewpager.setCurrentItem(index);
                }
            }
        });
        tv_left_arrow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (index > 0) {
                    index--;
                    my_viewpager.setCurrentItem(index);
                }
            }
        });
    }

    @Override
    public void onclick(int position, int id) {
        switch (id) {
            case 1:
                for (int i = 0; i < totalPage; i++) {
                    adapterlist.get(i).notifyDataSetChanged();
                }
                if (anInterface != null) {
                    anInterface.OnItmInterface(position);
                }
                break;
            case 2:
                if (anInterface != null) {
                    anInterface.OnItmInterface(position);
                }
                break;
        }

    }
}
