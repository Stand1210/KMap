package com.example.stand.kmap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.baidu.navisdk.adapter.PackageUtil.getSdcardDir;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String authBaseArr[] =
            {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION};
    private final static int authBaseRequestCode = 1;
    private final static int authComRequestCode = 2;
    private boolean hasInitSuccess = false;
    private boolean hasRequestComAuth = false;
    public static List<Activity> activityList = new LinkedList<Activity>();
    public static final String ROUTE_PLAN_NODE = "routePlanNode";
    private final static String authComArr[] = { Manifest.permission.READ_PHONE_STATE };





    MapView mMapView = null;
    private BaiduMap mBaiduMap;
    private Button mBtnMockNav;
    private Button mBtnRealNav;
    private Context context;

    //定位相关
    private LocationClient mLocationClient;
    private MyLocationListener mLocationListener;
    private boolean isFirstIn = true;
    private double mLatitude;
    private double mLongtitude;
    private LatLng mDestLocationData;     //导航目标地址



    private String mSDCardPath = null;
    public static final String APP_FOLDER_NAME = "BNSDKDemo";
    //自定义定位图标
    private BitmapDescriptor mIconLocation;
    private MyLocationConfiguration.LocationMode mLocationMode;
    private float mCurrentX;
    private MyOrientationlistener myOrientationlistener;

    //覆盖物相关
    private BitmapDescriptor mMarker;
    private RelativeLayout mMarkerLy;//设置布局
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        this.context = this;

        initView();
        //初始化定位
        initLocation();

        //导航按钮
        mBtnMockNav = (Button)findViewById(R.id.id_btn_mocknav);
        mBtnRealNav = (Button)findViewById(R.id.id_btn_realnav);

        mBtnMockNav.setOnClickListener(this);
        mBtnRealNav.setOnClickListener(this);
        //初始化覆盖
        initMarker();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        //长按设置目标地点时间监听
        mBaiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener()
        {

            @Override
            public void onMapLongClick(LatLng latLng) {
                Toast.makeText(MainActivity.this, "成功设置目的地", Toast.LENGTH_SHORT).show();
                mDestLocationData = latLng;
                addDestInfoOverLay(latLng);
            }
        });


        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Bundle extraInfo = marker.getExtraInfo();
                Info info = (Info) extraInfo.getSerializable("info");
                ImageView iv = (ImageView) mMarkerLy.findViewById(R.id.id_info_img);
                TextView distance = (TextView) mMarkerLy.findViewById(R.id.id_info_distance);
                TextView name = (TextView) mMarkerLy.findViewById(R.id.id_info_name);
                TextView zan = (TextView) mMarkerLy.findViewById(R.id.id_info_zan);
                iv.setImageResource(info.getImgId());
                distance.setText(info.getDistance());
                name.setText(info.getName());
                zan.setText(info.getZan() + "");

                //infoWindow
                InfoWindow infoWindow;
                TextView tv = new TextView(context);
                tv.setBackgroundResource(R.mipmap.location_tips);
                tv.setPadding(30, 20, 30, 50);
                tv.setText(info.getName());
                tv.setTextColor(Color.parseColor("#ffffff"));

                final LatLng latLng = marker.getPosition();
                //将经纬度转化为地图上的点
                Point p = mBaiduMap.getProjection().toScreenLocation(latLng);
                p.y -= 47;
                LatLng ll = mBaiduMap.getProjection().fromScreenLocation(p);

                infoWindow = new InfoWindow(tv, ll, 1);

                mBaiduMap.showInfoWindow(infoWindow);
                mMarkerLy.setVisibility(View.VISIBLE);
                return true;
            }
        });


        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMarkerLy.setVisibility(View.GONE);
                //隐藏infoWindow
                mBaiduMap.hideInfoWindow();
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        });

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();



        //初始化导航相关
        if (initDirs()) {

            initNavi();
        }
    }

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    String authinfo = null;



    private void initNavi() {


        BNOuterTTSPlayerCallback ttsCallback = null;

        // 申请权限
        if (Build.VERSION.SDK_INT >= 23) {

            if (!hasBasePhoneAuth()) {

                this.requestPermissions(authBaseArr, authBaseRequestCode);
                return;

            }
        }

        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME, new NaviInitListener() {
            @Override
            public void onAuthResult(int status, String msg) {
                if (0 == status) {
                    authinfo = "key校验成功!";
                } else {
                    authinfo = "key校验失败, " + msg;
                }
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, authinfo, Toast.LENGTH_LONG).show();
                    }
                });
            }

            public void initSuccess() {
                Toast.makeText(MainActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                hasInitSuccess = true;
                initSetting();
            }

            public void initStart() {
                Toast.makeText(MainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
            }

            public void initFailed() {
                Toast.makeText(MainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
            }

        }, null, ttsHandler, ttsPlayStateListener);

    }

    private void routeplanToNavi(boolean mock) {
        BNRoutePlanNode.CoordinateType coType = BNRoutePlanNode.CoordinateType.GCJ02;
        if (!hasInitSuccess) {
            Toast.makeText(MainActivity.this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }
        // 权限申请
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // 保证导航功能完备
            if (!hasCompletePhoneAuth()) {
                if (!hasRequestComAuth) {
                    hasRequestComAuth = true;
                    this.requestPermissions(authComArr, authComRequestCode);
                    return;
                } else {
                    Toast.makeText(MainActivity.this, "没有完备的权限!", Toast.LENGTH_SHORT).show();
                }
            }

        }
        BNRoutePlanNode sNode = null;
        BNRoutePlanNode eNode = null;

//        sNode = new BNRoutePlanNode(116.30142, 40.05087, "百度大厦", null, coType);
//        eNode = new BNRoutePlanNode(116.39750, 39.90882, "北京天安门", null, coType);

        sNode = new BNRoutePlanNode(mLongtitude, mLatitude, "我的地点", null, coType);
        eNode = new BNRoutePlanNode(mDestLocationData.longitude, mDestLocationData.latitude, "目标地点", null, coType);


        if (sNode != null && eNode != null) {
            List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
            list.add(sNode);
            list.add(eNode);
            BaiduNaviManager.getInstance().launchNavigator(this, list, 1, mock, new DemoRoutePlanListener(sNode));
        }
    }

    public class DemoRoutePlanListener implements BaiduNaviManager.RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
            /*
             * 设置途径点以及resetEndNode会回调该接口
             */


            Intent intent = new Intent(MainActivity.this, BNDemoGuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, (BNRoutePlanNode) mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);

        }

        @Override
        public void onRoutePlanFailed() {
            // TODO Auto-generated method stub
            Toast.makeText(MainActivity.this, "算路失败", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasCompletePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authComArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    // showToastMsg("Handler : TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    // showToastMsg("Handler : TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            // showToastMsg("TTSPlayStateListener : TTS play end");
        }

        @Override
        public void playStart() {
            // showToastMsg("TTSPlayStateListener : TTS play start");
        }
    };

    private void initSetting() {
        // BNaviSettingManager.setDayNightMode(BNaviSettingManager.DayNightMode.DAY_NIGHT_MODE_DAY);
        BNaviSettingManager
                .setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
        // BNaviSettingManager.setPowerSaveMode(BNaviSettingManager.PowerSaveMode.DISABLE_MODE);
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);
    }

    private boolean hasBasePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void addDestInfoOverLay(LatLng latLng) {
        mBaiduMap.clear();
        OverlayOptions options = new MarkerOptions().position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.maker))
                .zIndex(5);
        mBaiduMap.addOverlay(options);
    }

    private void initMarker() {
        mMarker = BitmapDescriptorFactory.fromResource(R.mipmap.maker);
        mMarkerLy = (RelativeLayout) findViewById(R.id.id_maker_ly);
    }

    private void initLocation() {

        mLocationMode = MyLocationConfiguration.LocationMode.NORMAL;
        mLocationClient = new LocationClient(this);
        mLocationListener = new MyLocationListener();
        //注册
        mLocationClient.registerLocationListener(mLocationListener);

        LocationClientOption option = new LocationClientOption();
        option.setCoorType("bd09ll");
        option.setIsNeedAddress(true);
        option.setOpenGps(true);
        //刷新间隔
        option.setScanSpan(1000);
        mLocationClient.setLocOption(option);

        //初始化图标
        mIconLocation = BitmapDescriptorFactory.fromResource(R.mipmap.arrow);

        //初始化方向监听器
        myOrientationlistener = new MyOrientationlistener(context);

        myOrientationlistener.setOnOrientationListener(new MyOrientationlistener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        MapStatusUpdate msu = MapStatusUpdateFactory.zoomTo(15.0f);
        mBaiduMap.setMapStatus(msu);
    }


    protected void onStart() {
        super.onStart();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        //开启定位
        mBaiduMap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
        }

        //开启方向传感器
        myOrientationlistener.start();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    protected void onStop() {
        super.onStop();// ATTENTION: This was auto-generated to implement the App Indexing API.
// See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());

        //停止定位
        mBaiduMap.setMyLocationEnabled(false);
        mLocationClient.stop();
        //停止方向传感器
        myOrientationlistener.stop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.id_map_common:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                break;

            case R.id.id_map_site:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;

            case R.id.id_map_traffic:
                if (mBaiduMap.isTrafficEnabled()) {
                    mBaiduMap.setTrafficEnabled(false);
                    item.setTitle("实时交通(off)");
                } else {
                    mBaiduMap.setTrafficEnabled(true);
                    item.setTitle("实时交通(on)");
                }
                break;

            case R.id.id_map_location:
                centerToMyLocation();
                break;

            case R.id.id_map_mode_common:
                mLocationMode = MyLocationConfiguration.LocationMode.NORMAL;
                break;

            case R.id.id_map_mode_following:
                mLocationMode = MyLocationConfiguration.LocationMode.FOLLOWING;
                break;

            case R.id.id_map_mode_compass:
                mLocationMode = MyLocationConfiguration.LocationMode.COMPASS;
                break;

            case R.id.id_add_overlay:
                addOverlays(Info.infos);

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * @param infos 添加覆盖物
     */
    private void addOverlays(List<Info> infos) {
        mBaiduMap.clear();
        LatLng latLng = null;
        Marker marker = null;
        OverlayOptions options;
        for (Info info : infos) {
            //经纬度
            latLng = new LatLng(info.getLatitude(), info.getLongitude());

            //图标
            options = new MarkerOptions().position(latLng).icon(mMarker).zIndex(5);

            marker = (Marker) mBaiduMap.addOverlay(options);
            Bundle arg0 = new Bundle();
            arg0.putSerializable("info", info);
            marker.setExtraInfo(arg0);
        }
        //视角切换为覆盖物的位置
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.setMapStatus(msu);
    }

    //定位到我的位置
    private void centerToMyLocation() {
        LatLng latLng = new LatLng(mLatitude, mLongtitude);
        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
        mBaiduMap.animateMapStatus(msu);
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.id_btn_mocknav:
                if (mDestLocationData == null) {
                    Toast.makeText(MainActivity.this, "请先设置目标地点", Toast.LENGTH_SHORT).show();
                    return;
                }
                routeplanToNavi(false);
                break;

            case R.id.id_btn_realnav:
                if (mDestLocationData == null) {
                    Toast.makeText(MainActivity.this, "请先设置目标地点", Toast.LENGTH_SHORT).show();
                    return;
                }
                routeplanToNavi(true);
                break;
        }
    }

    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            MyLocationData data = new MyLocationData.Builder()//    拿到定位数据
                    .direction(mCurrentX)//在获得定位之后回掉更新方向传感器箭头指向
                    .accuracy(location.getRadius())//
                    .latitude(location.getLatitude())//
                    .longitude(location.getLongitude())//
                    .build();
            mBaiduMap.setMyLocationData(data);  //用拿到的定位数据更新当前位置

            //设置自定义图标
            MyLocationConfiguration config = new MyLocationConfiguration(mLocationMode, true, mIconLocation);
            mBaiduMap.setMyLocationConfigeration(config);

            //更新经纬度
            mLatitude = location.getLatitude();
            mLongtitude = location.getLongitude();


            if (isFirstIn) {    //第一次定位的话更新当前显示为所定位的位置
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(msu);
                isFirstIn = false;

//                Toast.makeText(context, location.getAddrStr(), Toast.LENGTH_SHORT).
            }
        }


        @Override
        public void onConnectHotSpotMessage(String s, int i) {

        }
    }
}
