package com.example.corekotlinremaster

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import com.baidu.location.BDAbstractLocationListener
import com.baidu.location.BDLocation
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.map.MyLocationData
import com.baidu.mapapi.model.LatLng
import com.example.corekotlinremaster.BluetoothActivity.Companion.BLUE_ADDRESS
import com.example.corekotlinremaster.BluetoothActivity.Companion.BLUE_NAME
import com.example.corekotlinremaster.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {

    var mMapView: MapView? = null
    var mBaiduMap: BaiduMap? = null
    var mLocationClient: LocationClient? = null
    var selfNumber: String? = null

    private lateinit var binding: ActivityMainBinding

    lateinit var blueAddress: String
    lateinit var blueName: String
    var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    var mBluetoothSocket: BluetoothSocket? = null
    lateinit var mBluetoothAdapter: BluetoothAdapter
    var isBlueConnected: Boolean = false
    var isBtInfoAcknowledged: Boolean = false
    val MESSAGE_RECEIVE_TAG = 111
    private val BUNDLE_RECEIVE_DATA = "ReceiveData"
    //设置发送和接收的字符编码格式
    private val ENCODING_FORMAT = "GBK"

    private val TAG = "MainActivity"
    private var isFirstLocation = true

    private var permissionAllGranted = 0 //用于标志权限是否全被获取到
    lateinit var reqPermissionLauncher: ActivityResultLauncher<Array<String>> //权限请求
    //声明定位权限变量数组
    private var permissionsForLocation: Array<String> =
        arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.INTERNET
        )
    //声明蓝牙权限变量数组
    private var permissionsForBluetooth: Array<String> =
        arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        )
    //声明存储权限变量数组
    private var permissionsForStorage: Array<String> =
        arrayOf<String>(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    //声明录音权限变量数组
    private var permissionsForRecord: Array<String> =
        arrayOf<String>(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()//使用启动页相关必须，即使非启动页也需要使用

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //设置亮色状态栏模式
        val controller = ViewCompat.getWindowInsetsController(this.window.decorView)
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true

        mMapView = findViewById(R.id.bmapView)

        //获取权限
        reqPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions : Map<String, Boolean> ->
                // Do something if some permissions granted or denied
                Log.d(TAG, "reqPermissionLauncher: active")
                permissions.entries.forEach {
                    // Do checking here
                    Log.d(TAG, "onCreate: ${it.key}")
                }
            }
        //主activity的启动仅需要定位权限，其余权限分开获取
        checkPermissions(permissionsForLocation)

        //UI初始化
        binding.btSwitch.setOnCheckedChangeListener { _, b ->
            if(binding.btSwitch.isChecked && !isBlueConnected && !isBtInfoAcknowledged){
                //checkPermissions(permissionsForBluetooth)
                val intent = Intent(this,  BluetoothActivity::class.java)
                startActivity(intent)
            }else if (binding.btSwitch.isChecked && isBtInfoAcknowledged) {

                //开始连接蓝牙
                funStartBlueClientConnect()
                //打开蓝牙接收消息
                funBlueClientStartReceive()
            } else if(!binding.btSwitch.isChecked && isBlueConnected) {
                disconnect()
            }
        }
        binding.btMessage.movementMethod = ScrollingMovementMethod.getInstance()

        mBaiduMap = mMapView?.map
        mBaiduMap?.isMyLocationEnabled=true
        //声明LocationClient类
        //解决隐私政策报错
        LocationClient.setAgreePrivacy(true)
        mLocationClient = LocationClient(applicationContext)
        //注册监听函数
        mLocationClient?.registerLocationListener(MyLocationListener())
        initLocation()
        mLocationClient?.start()


    }

    inner class MyLocationListener : BDAbstractLocationListener() {
        override fun onReceiveLocation(location: BDLocation?) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return
            }

            val locData = MyLocationData.Builder()
                //.accuracy(location.radius) // 此处设置开发者获取到的方向信息，顺时针0-360
                .direction(location.direction)
                .latitude(location.latitude)
                .longitude(location.longitude).build()
            Log.d(TAG, "onReceiveLocation: ${location.latitude} and ${location.longitude}")
            //拼凑自身的定位数据包信息
            selfNumber = "1"
            var messageString = "#$selfNumber,${location.latitude},${location.longitude},"
            messageString += "\r\n"

            //初次定位导航至定位结果位置
            navigate(location)
            mBaiduMap?.setMyLocationData(locData)
            if(isBlueConnected){
                funBlueClientSend(messageString)
            }

        }
    }

    /**
     * 初次定位的坐标移动
     */
    private fun navigate(location: BDLocation) {
        if(isFirstLocation) {
            Toast.makeText(this, "nav to : ${location.addrStr}", Toast.LENGTH_SHORT).show()
            val result = LatLng(location.latitude, location.longitude)
            var update = MapStatusUpdateFactory.newLatLng(result)
            mBaiduMap!!.animateMapStatus(update)
            update = MapStatusUpdateFactory.zoomTo(16f)
            mBaiduMap!!.animateMapStatus(update)
            if(location.latitude > 0.0001 && location.longitude > 0.0001){
                isFirstLocation = false
            }
        }
    }
    /**
    * 定位方法的可选设置
     */
    private fun initLocation() {
        val option = LocationClientOption()
        //注意这个初始化方法，切忌赋予一个空值，会导致下面的参数失效
        option.locationMode = LocationClientOption.LocationMode.Hight_Accuracy
        //LocationMode. Device_Sensors：仅使用设备；  //可选，设置定位模式，默认高精度
        //LocationMode.Hight_Accuracy：高精度；
        //LocationMode. Battery_Saving：低功耗;
        option.setCoorType("bd09ll")
        //可选，设置返回经纬度坐标类型，默认GCJ02
        //GCJ02：国测局坐标；
        //BD09ll：百度经纬度坐标；
        //BD09：百度墨卡托坐标；
        //海外地区定位，无需设置坐标类型，统一返回WGS84类型坐标
        option.setScanSpan(10000)
        //可选，设置发起定位请求的间隔，int类型，单位ms
        //如果设置为0，则代表单次定位，即仅定位一次，默认为0
        //如果设置非0，需设置1000ms以上才有效
        option.isOpenGps = true
        //可选，设置是否使用gps，默认false
        //使用高精度和仅用设备两种定位模式的，参数必须设置为true
        option.isLocationNotify = false
        //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
        option.setIgnoreKillProcess(true)
        //可选，定位SDK内部是一个service，并放到了独立进程。
        //设置是否在stop的时候杀死这个进程，默认（建议）不杀死，即setIgnoreKillProcess(true)
        option.SetIgnoreCacheException(false)
        //可选，设置是否收集Crash信息，默认收集，即参数为false
        option.setWifiCacheTimeOut(5 * 60 * 1000)
        //可选，V7.2版本新增能力
        //如果设置了该接口，首次启动定位时，会先判断当前Wi-Fi是否超出有效期，若超出有效期，会先重新扫描Wi-Fi，然后定位
        option.setEnableSimulateGps(false)
        //可选，设置是否需要过滤GPS仿真结果，默认需要，即参数为false
        option.setNeedNewVersionRgc(true)
        //可选，设置是否需要最新版本的地址信息。默认不需要，即参数为false
        mLocationClient!!.locOption = option
        //mLocationClient为第二步初始化过的LocationClient对象
        //需将配置好的LocationClientOption对象，通过setLocOption方法传递给LocationClient对象使用
        //更多LocationClientOption的配置，请参照类参考中LocationClientOption类的详细说明
    }

    private fun checkPermissions(permissionArrays: Array<String>) : Boolean {
        // 检查该权限是否已经获取
        for (request in permissionArrays){
            if(ContextCompat.checkSelfPermission(this, request)
                == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "$request is granted")
            } else{
                permissionAllGranted++
            }
        }
        return if (permissionAllGranted != 0){
            reqPermissionLauncher.launch(permissionArrays)
            permissionAllGranted = 0
            false
        }else
            true

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        //获取蓝牙设备的名字和地址
        blueAddress = intent?.getStringExtra(BLUE_ADDRESS)!!
        blueName = intent.getStringExtra(BLUE_NAME)!!
        isBtInfoAcknowledged = true
        Log.d("MainActivity", "address: $blueAddress; name: $blueName")
        //开始连接蓝牙
        funStartBlueClientConnect()
        //打开蓝牙接收消息
        funBlueClientStartReceive()
    }

    //开始连接蓝牙
    private fun funStartBlueClientConnect() {
        thread {
            try {
                //这一段代码必须在子线程处理，直接使用协程会阻塞主线程，所以用Thread,其实也可以直接用Thread，不用协程
                if (mBluetoothSocket == null || !isBlueConnected) {
                    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = mBluetoothAdapter.getRemoteDevice(blueAddress)
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.d(TAG, "funStartBlueClientConnect: 蓝牙权限异常")
                        return@thread
                    }
                    mBluetoothSocket =
                        device.createInsecureRfcommSocketToServiceRecord(myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    mBluetoothSocket!!.connect()
                    isBlueConnected = true
                }
            } catch (e: IOException) {
                //连接失败销毁Activity
                finish()
                e.printStackTrace()
            }
        }
    }

    //打开蓝牙接收消息
    private fun funBlueClientStartReceive() {
        thread {
            while (true) {
                //启动蓝牙接收消息
                //注意,如果不在子线程或者协程进行，会导致主线程阻塞，无法绘制
                try {
                    if (mBluetoothSocket != null) {
                        if (mBluetoothSocket!!.isConnected) {
                            Log.e("eee", "现在可以接收数据了")
                            receiveMessage()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e(TAG, "funBlueClientStartReceive:" + e.toString())
                }
            }
        }
    }

    //蓝牙接收消息的函数体
    private fun receiveMessage() {
        val mmInStream: InputStream = mBluetoothSocket!!.inputStream
        val mmBuffer = ByteArray(1024) // mmBuffer store for the stream
        var bytes: Int
        //java.lang.OutOfMemoryError: pthread_create (1040KB stack) failed: Try again
        //已经Thread了就不要再次thread了
        //   thread {
        while (true) {
            // Read from the InputStream.
            try {
                bytes = mmInStream.read(mmBuffer)
            } catch (e: IOException) {
                Log.d(TAG, "Input stream was disconnected", e)
                break
            }
            val message = Message()
            val bundle = Bundle()
            //默认GBK编码
            val string = String(mmBuffer, 0, bytes, Charset.forName(ENCODING_FORMAT))
            bundle.putString(BUNDLE_RECEIVE_DATA, string)
            message.what = MESSAGE_RECEIVE_TAG
            message.data = bundle
            handler.sendMessage(message)
            Log.e("receive", string)
        }
        //  }
    }

    //蓝牙发送消息
    private fun funBlueClientSend(input: String) {
        thread {
            if (mBluetoothSocket != null && isBlueConnected) {
                try {
                    mBluetoothSocket!!.outputStream.write(input.toByteArray(Charset.forName(ENCODING_FORMAT)))
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(TAG, "sendCommand: 发送消息失败", e)
                }
            }
        }
    }

    //这是官方推荐的方法
    val stringBuffer = StringBuffer()
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_RECEIVE_TAG -> {
                    val string = stringBuffer.append(msg.data.getString(BUNDLE_RECEIVE_DATA))
                    binding.btMessage.text = string
                }
            }
        }
    }

    //蓝牙断开连接
    private fun disconnect() {
        if (mBluetoothSocket != null) {
            try {
                mBluetoothSocket!!.close()
                mBluetoothSocket = null
                isBlueConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "disconnect: 蓝牙关闭失败", e)
            }
        }
    }

    override fun onResume() {

        super.onResume()
        mMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onDestroy() {
        //释放
        mLocationClient?.stop()
        mBaiduMap?.isMyLocationEnabled = false
        mMapView?.onDestroy()
        mMapView = null
        super.onDestroy()

    }

}
