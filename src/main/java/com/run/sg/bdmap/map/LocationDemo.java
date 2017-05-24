package com.run.sg.bdmap.map;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.RouteLine;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.run.sg.bdmap.R;
import com.run.sg.bdmap.about_us.About_Us_Activity;
import com.run.sg.bdmap.found.foundDialogListviewAdapter;
import com.run.sg.bdmap.search.RouteLineAdapter;
import com.run.sg.bdmap.search.RoutePlanDemo;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.run.sg.bdmap.util.MapConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;

/**
 * 此demo用来展示如何结合定位SDK实现定位，并使用MyLocationOverlay绘制定位位置 同时展示如何使用自定义图标绘制并点击时弹出泡泡
 */
public class LocationDemo extends Activity implements SensorEventListener, OnGetGeoCoderResultListener {

    // 搜索相关
    RoutePlanSearch mSearchRoute = null;    // 搜索模块，也可去掉地图模块独立使用
    boolean hasShownDialogue = false;
    WalkingRouteResult nowResultwalk = null;
    PlanNode stNode, edNode;
    RouteLine route = null;

    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    GeoCoder mSearch = null; // 搜索模块，也可去掉地图模块独立使用
    private LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;
    private SensorManager mSensorManager;
    private Double lastX = 0.0;
    private int mCurrentDirection = 0;
    private double mCurrentLat = 0.0;
    private double mCurrentLon = 0.0;
    private float mCurrentAccracy;

    MapView mMapView;
    BaiduMap mBaiduMap;

    // UI相关
    OnCheckedChangeListener radioButtonListener;
    boolean isFirstLoc = true; // 是否首次定位
    private MyLocationData locData;
    private float direction;

    //广告轮播相关
    private ViewPager viewPager; // 显示轮播图
    private LinearLayout imgTipsLayout; // 显示小圆点
    private List<ImageView> imageViewList = new ArrayList<ImageView>(); // 装载轮播图
    private List<ImageView> imageViewTips; // 装载小圆点
    private String[] imageUrls = null; // 网络图片资源
    private ImageLoader imageLoader; // 图片加载器
    private DisplayImageOptions options;// 图片展示配置
    private ScheduledExecutorService scheduled; // 实例化线程池对象
    private ScheduledExecutorService timeScheduled; // 实例化线程池对象
    private TimerTask task; // 定时器任务
    private int oldPage = 0;  //前一页
    private int nextPage = 1;  //下一页
    private boolean isPause = false; //是否触发暂停

    //Main UI
    ListView mFoundDialogListview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_location);

        //广告初始化
        viewPager = (ViewPager) this.findViewById(R.id.viewpager);
        imgTipsLayout = (LinearLayout) this.findViewById(R.id.imgTipsLayout);
        // 初始化图片资源
        initImageViewList();
        // 初始化圆点
        initImageViewTips();
        // 初始化ImageLoader
        initImageLoader();
        viewPager.setFocusable(true); // 设置焦点
        viewPager.setAdapter(new MyPagerAdapter());
        viewPager.setOnPageChangeListener(new MyViewPagerChangeListener());
        // 设置默认从1开始
        viewPager.setCurrentItem(1);

        // 开启定时器，每隔2秒自动播放下一张（通过调用线程实现）（与Timer类似，可使用Timer代替）
        scheduled = Executors.newSingleThreadScheduledExecutor();
        // 设置一个线程，该线程用于通知UI线程变换图片
        ViewPagerTask pagerTask = new ViewPagerTask();
        scheduled.scheduleAtFixedRate(pagerTask, 2, 4, TimeUnit.SECONDS);

        timeScheduled = Executors.newSingleThreadScheduledExecutor();
        task = new TimerTask() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                isPause = false;
            }
        };

        initBottomClickListener();

        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//获取传感器管理服务
        mCurrentMode = LocationMode.NORMAL;
        mBaiduMap.setMyLocationConfiguration(new MyLocationConfiguration(
                mCurrentMode, true, mCurrentMarker));
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.overlook(0);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        mBaiduMap.setOnMapTouchListener(new BaiduMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                if (mFoundDialogListview != null && mFoundDialogListview.getVisibility() == View.VISIBLE){
                    mFoundDialogListview.setVisibility(View.GONE);
                }
            }
        });
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        //option.setScanSpan(1000);
        mLocClient.setLocOption(option);
        mLocClient.start();

        // 初始化搜索模块，注册事件监听
        mSearch = GeoCoder.newInstance();
        mSearch.setOnGetGeoCodeResultListener(this);
       // 初始化搜索模块，注册事件监听
        mSearchRoute = RoutePlanSearch.newInstance();
        mSearchRoute.setOnGetRoutePlanResultListener(mGetRoutePlanResultListener);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {
            mCurrentDirection = (int) x;
            locData = new MyLocationData.Builder()
                    .accuracy(mCurrentAccracy)
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection)
                    .latitude(mCurrentLat)
                    .longitude(mCurrentLon)
                    .build();
            mBaiduMap.setMyLocationData(locData);
        }
        lastX = x;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * 发起搜索
     *
     * @param v
     */
    public void searchButtonProcess(View v) {
        /*if (v.getId() == R.id.geocode) {
            EditText editCity = (EditText) findViewById(R.id.city);
            EditText editGeoCodeKey = (EditText) findViewById(R.id.geocodekey);
            // Geo搜索
            mSearch.geocode(new GeoCodeOption().city(
                    editCity.getText().toString()).address(editGeoCodeKey.getText().toString()));
        }*/
    }

    /**
     * 定位SDK监听函数
     */
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            mCurrentLat = location.getLatitude();
            mCurrentLon = location.getLongitude();
            mCurrentAccracy = location.getRadius();
            locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(mCurrentDirection)
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .build();
            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
            stNode = PlanNode.withLocation(new LatLng(location.getLatitude(),location.getLongitude()));
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    @Override
    protected void onResume () {
        mMapView.onResume();
        super.onResume();
        //为系统的方向传感器注册监听器
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause () {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop () {
        //取消注册传感器监听
        mSensorManager.unregisterListener(this);
        super.onStop();
    }

    @Override
    protected void onDestroy () {
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult result) {
        if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
            Toast.makeText(this, "抱歉，未能找到结果", Toast.LENGTH_LONG).show();
            return;
        }
        mBaiduMap.clear();
//        mBaiduMap.addOverlay(new MarkerOptions().position(result.getLocation())
//                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_marka)));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(result.getLocation()));
        /*String strInfo = String.format("纬度：%f 经度：%f",
                result.getLocation().latitude, result.getLocation().longitude);
        Toast.makeText(this, strInfo, Toast.LENGTH_LONG).show();*/

        edNode = PlanNode.withLocation(result.getLocation());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSearchRoute.walkingSearch((new WalkingRoutePlanOption())
                        .from(stNode).to(edNode));
            }
        },1000);

    }

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {

    }

    OnGetRoutePlanResultListener mGetRoutePlanResultListener = new OnGetRoutePlanResultListener() {
        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult result) {
            if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                Toast.makeText(LocationDemo.this, "抱歉，未找到结果", Toast.LENGTH_SHORT).show();
            }
            if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                // 起终点或途经点地址有岐义，通过以下接口获取建议查询信息
                // result.getSuggestAddrInfo()
                return;
            }
            if (result.error == SearchResult.ERRORNO.NO_ERROR) {

                if (result.getRouteLines().size() > 1) {
                    nowResultwalk = result;
                    if (!hasShownDialogue) {
                        MyTransitDlg myTransitDlg = new MyTransitDlg(LocationDemo.this,
                                result.getRouteLines(),
                                RouteLineAdapter.Type.WALKING_ROUTE);
                        myTransitDlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                hasShownDialogue = false;
                            }
                        });
                        myTransitDlg.setOnItemInDlgClickLinster(new OnItemInDlgClickListener() {
                            public void onItemClick(int position) {
                                route = nowResultwalk.getRouteLines().get(position);
                                WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaiduMap);
                                mBaiduMap.setOnMarkerClickListener(overlay);
                                overlay.setData(nowResultwalk.getRouteLines().get(position));
                                overlay.addToMap();
                                overlay.zoomToSpan();
                            }
                        });
                        myTransitDlg.show();
                        hasShownDialogue = true;
                    }
                } else if (result.getRouteLines().size() == 1) {
                    // 直接显示
                    route = result.getRouteLines().get(0);
                    WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaiduMap);
                    mBaiduMap.setOnMarkerClickListener(overlay);
                    overlay.setData(result.getRouteLines().get(0));
                    overlay.addToMap();
                    overlay.zoomToSpan();
                } else {
                    Log.d("route result", "结果数<0");
                    return;
                }
        }

        }


        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

        }
    };

    // 供路线选择的Dialog
    class MyTransitDlg extends Dialog {

        private List<? extends RouteLine> mtransitRouteLines;
        private ListView transitRouteList;
        private RouteLineAdapter mTransitAdapter;

        OnItemInDlgClickListener onItemInDlgClickListener;

        public MyTransitDlg(Context context, int theme) {
            super(context, theme);
        }

        public MyTransitDlg(Context context, List<? extends RouteLine> transitRouteLines, RouteLineAdapter.Type
                type) {
            this(context, 0);
            mtransitRouteLines = transitRouteLines;
            mTransitAdapter = new RouteLineAdapter(context, mtransitRouteLines, type);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        @Override
        public void setOnDismissListener(OnDismissListener listener) {
            super.setOnDismissListener(listener);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_transit_dialog);

            transitRouteList = (ListView) findViewById(R.id.transitList);
            transitRouteList.setAdapter(mTransitAdapter);

            transitRouteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    onItemInDlgClickListener.onItemClick(position);
                    dismiss();
                    hasShownDialogue = false;
                }
            });
        }

        public void setOnItemInDlgClickLinster(OnItemInDlgClickListener itemListener) {
            onItemInDlgClickListener = itemListener;
        }

    }

    // 响应DLg中的List item 点击
    interface OnItemInDlgClickListener {
        void onItemClick(int position);
    }

    private class MyWalkingRouteOverlay extends WalkingRouteOverlay {

        public MyWalkingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_st);
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
            return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);
        }
    }

    /**
     * 初始化图片list
     */
    private void initImageViewList() {

        /*imageUrls = new String[] {
                "http://h.hiphotos.baidu.com/image/pic/item/9825bc315c6034a8d141851dce1349540823768e.jpg",
                "http://e.hiphotos.baidu.com/image/pic/item/f9198618367adab49eb71beb8ed4b31c8701e437.jpg",
                "http://d.hiphotos.baidu.com/image/pic/item/9345d688d43f8794675c75b2d71b0ef41ad53a8e.jpg",
                "http://a.hiphotos.baidu.com/image/pic/item/55e736d12f2eb938d3de795ad0628535e4dd6fe2.jpg",
                "http://h.hiphotos.baidu.com/image/pic/item/9825bc315c6034a8d141851dce1349540823768e.jpg",
                "http://e.hiphotos.baidu.com/image/pic/item/f9198618367adab49eb71beb8ed4b31c8701e437.jpg" };*/

        imageUrls = new String[] {
                "advertise_pic_6",
                "advertise_pic_1","advertise_pic_2","advertise_pic_3",
                "advertise_pic_4","advertise_pic_5","advertise_pic_6"
                ,"advertise_pic_1"
        };

        Resources res = this.getResources();

        Integer[] drawableId = new Integer[]{
                R.drawable.advertise_pic_6,
                R.drawable.advertise_pic_1,R.drawable.advertise_pic_2,
                R.drawable.advertise_pic_3,R.drawable.advertise_pic_4,
                R.drawable.advertise_pic_5,R.drawable.advertise_pic_6,
                R.drawable.advertise_pic_1
        };

        for (int i = 0; i < imageUrls.length; i++) {
            ImageView imageView = new ImageView(LocationDemo.this);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            //imageView.setTag(imageUrls[i]);
            Drawable draw = res.getDrawable(drawableId[i],null);
            imageView.setImageDrawable(draw);
            imageViewList.add(imageView);
        }
    }

    /**
     * 初始化圆点
     */
    private void initImageViewTips() {

        imageViewTips = new ArrayList<ImageView>();

        for (int i = 0; i < imageUrls.length; i++) {
            ImageView imageViewTip = new ImageView(LocationDemo.this);
            imageViewTip.setLayoutParams(new ViewGroup.LayoutParams(10, 10)); // 设置圆点宽高
            imageViewTips.add(imageViewTip);
            if (i == 0 || i == imageUrls.length - 1) {
                imageViewTip.setVisibility(View.GONE);

            } else if (i == 1) {
                imageViewTip
                        .setBackgroundResource(R.drawable.page_indicator_focused);
            } else {
                imageViewTip
                        .setBackgroundResource(R.drawable.page_indicator_unfocused);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    new ViewGroup.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
            params.leftMargin = 5;
            params.rightMargin = 5;
            imgTipsLayout.addView(imageViewTip, params);
        }
    }

    /**
     * 初始化ImageLoader
     */
    private void initImageLoader() {
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration
                .createDefault(LocationDemo.this));

        options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.default_icon) // 加载中的默认图片
				.showImageForEmptyUri(R.drawable.default_icon) // 加载错误的默认图片
				.showImageOnFail(R.drawable.default_icon) // 加载失败时的默认图片
				.cacheInMemory(true)// 开启内存缓存
                .cacheOnDisk(true) // 开启硬盘缓存
                .resetViewBeforeLoading(false).build();
    }

    private class ViewPagerTask implements Runnable {
        @Override
        public void run() {
            // 发送消息给UI线程
            System.out.println(nextPage);
            if (!isPause) { //不是暂停状态
                handler.sendEmptyMessage(nextPage);
            }

        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 接收到消息后，更新页面
            viewPager.setCurrentItem(msg.what);
        }
    };

    /**
     * ViewPager适配器
     */
    private class MyPagerAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return imageViewList.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            // TODO Auto-generated method stub
            return arg0 == arg1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            // TODO Auto-generated method stub
            container.removeView(imageViewList.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, final int position) {
            // TODO Auto-generated method stub

            final ImageView imageView = imageViewList.get(position);
            /*imageLoader.displayImage(imageView.getTag().toString(), imageView,
                    options);*/

            imageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    //Toast.makeText(getApplicationContext(), imageView.toString(), 0).show();
                }
            });

            container.addView(imageViewList.get(position));
            return imageViewList.get(position);
        }

    }

    /**
     * ViewPager事件监听器
     */
    private class MyViewPagerChangeListener implements OnPageChangeListener {

        @Override
        public void onPageScrollStateChanged(int state) {
            // TODO Auto-generated method stub
            switch (state) {
                case 1: //1表示手动触摸
                    isPause = true;
                    if (!timeScheduled.isTerminated()) {
                        timeScheduled.schedule(task, 6, TimeUnit.SECONDS); //让自动循环暂停6秒
                    }
                    break;
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
            // TODO Auto-generated method stub
            if (position == imageViewList.size() - 1 && positionOffset == 0.0f) {
                viewPager.setCurrentItem(1, false);

            } else if (position == 0 && positionOffset == 0.0f) {
                viewPager.setCurrentItem(imageViewList.size() - 2, false);
            }
        }

        @Override
        public void onPageSelected(int position) {
            // TODO Auto-generated method stub
            if (position == imageViewList.size() - 1) {
                // 设置当前位置小红点的背景
                nextPage = 2;
                imageViewTips.get(1).setBackgroundResource(
                        R.drawable.page_indicator_focused);
                // 改变前一个位置小红点的背景
                imageViewTips.get(oldPage).setBackgroundResource(
                        R.drawable.page_indicator_unfocused);
                oldPage = 1;
            } else if (position == 0) {
                nextPage = imageViewList.size() - 2;
                // 改变前一个位置小红点的背景
                imageViewTips.get(imageViewTips.size() - 2)
                        .setBackgroundResource(
                                R.drawable.page_indicator_focused);
                // 改变前一个位置小红点的背景
                imageViewTips.get(oldPage).setBackgroundResource(
                        R.drawable.page_indicator_unfocused);
                oldPage = imageViewTips.size() - 2;
            } else {
                // 改变前一个位置小红点的背景
                imageViewTips.get(position).setBackgroundResource(
                        R.drawable.page_indicator_focused);
                if (position != oldPage) {
                    imageViewTips.get(oldPage).setBackgroundResource(
                            R.drawable.page_indicator_unfocused);
                }
                nextPage = position + 1;
                oldPage = position;
            }
        }

    }

    private View.OnClickListener mButtomBarClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            switch (id){
                case R.id.found_layout:
                    if (mFoundDialogListview != null){
                        mFoundDialogListview.setVisibility(
                                (mFoundDialogListview.getVisibility() == View.VISIBLE)
                                        ? View.GONE : View.VISIBLE);
                    }
                    break;
                case R.id.good_you_layout:
                    if (mFoundDialogListview != null && mFoundDialogListview.getVisibility() == View.VISIBLE){
                        mFoundDialogListview.setVisibility(View.GONE);
                    }
                    break;
                case R.id.about_us_layout:
                    if (mFoundDialogListview != null && mFoundDialogListview.getVisibility() == View.VISIBLE){
                        mFoundDialogListview.setVisibility(View.GONE);
                    }
                    Intent intent = new Intent(LocationDemo.this, About_Us_Activity.class);
                    startActivity(intent);
                    break;
                default:
                    if (mFoundDialogListview != null && mFoundDialogListview.getVisibility() == View.VISIBLE){
                        mFoundDialogListview.setVisibility(View.GONE);
                    }
                    break;
            }

        }
    };

    private void initBottomClickListener(){
        findViewById(R.id.found_layout).setOnClickListener(mButtomBarClickListener);
        findViewById(R.id.good_you_layout).setOnClickListener(mButtomBarClickListener);
        findViewById(R.id.about_us_layout).setOnClickListener(mButtomBarClickListener);
        mFoundDialogListview = (ListView)findViewById(R.id.found_dialog_listview);
        mFoundDialogListview.setAdapter(new foundDialogListviewAdapter(this));
        mFoundDialogListview.setOnItemClickListener(mItemClickListener);
        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapConstants.SEARCH_ACTION);
                startActivity(intent);
            }
        });
    }

    private ListView.OnItemClickListener mItemClickListener = new ListView.OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position){
                case 0:
                    Toast.makeText(LocationDemo.this,"捐赠点\n Clicked",Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(LocationDemo.this,"敬老院\n Clicked",Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(LocationDemo.this,"失物招领处\n Clicked",Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(LocationDemo.this,"福利院\n Clicked",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(LocationDemo.this,"onItemClick\n Clicked",Toast.LENGTH_SHORT).show();
                    break;
            }
            if (mFoundDialogListview != null && mFoundDialogListview.getVisibility() == View.VISIBLE){
                mFoundDialogListview.setVisibility(View.GONE);
            }
        }
    };

    @Override
    public void onBackPressed() {
        if (mFoundDialogListview != null && mFoundDialogListview.getVisibility() == View.VISIBLE){
            mFoundDialogListview.setVisibility(View.GONE);
        }
        super.onBackPressed();
    }
}
