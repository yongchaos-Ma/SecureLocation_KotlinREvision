package com.example.corekotlinremaster

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.corekotlinremaster.databinding.ActivityBluetoothBinding

/**
 * @author Zhang Xingkun
 *
 * @note 这个Activity的主要作用就是获取已经连接的蓝牙设备目录，然后通过点击
 * 设备，获取mac地址，建立连接，同时如果没有打开蓝牙，提示打开蓝牙
 *
 * @tips 蓝牙建立通信,最好不要在App里面扫描未连接的蓝牙设备去连接，
 * 因为高版本的Android对安全性有了更高的要求，如果在App里面连接了陌生的蓝牙设备，
 * 那么关闭App时，相应的蓝牙设备也会关闭，并且不会保存在系统的蓝牙设备列表里面，
 * 某些情况下，可能还会导致连接问题，
 * 比如如果App内连接Wifi，一定概率导致socket客户端建立失败
 * 初学者碰到这种问题往往可能会很迷惑，毕竟这是版本更新的问题
 * 所以如果建立连接的相关代码正确，可是却一直无法建立连接，倒不妨考虑一下是不是权限的问题，
 * 毕竟这些有可能时默认禁止的，即使在xml中添加了权限也不一定有用
 */

class BluetoothActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBluetoothBinding
    private val TAG = "BluetoothActivity"

    private var myBluetoothAdapter: BluetoothAdapter? = null
    lateinit var bluetoothManager: BluetoothManager
    private lateinit var myPairedDevices: Set<BluetoothDevice>
    private val requestEnableBlue = 1
    private var isSupportBlue = true
    companion object {
        const val BLUE_ADDRESS: String = "DeviceAddress"
        const val BLUE_NAME: String = "DeviceName"

    }

    private var permissionAllGranted = 0 //用于标志权限是否全被获取到
    lateinit var reqPermissionLauncher: ActivityResultLauncher<Array<String>> //权限请求
    //声明蓝牙权限变量数组
    private var permissionsForBluetooth: Array<String> =
        arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //android12及以上添加权限
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            Log.d(TAG, "buildVersion: ${Build.VERSION.SDK_INT}")
            permissionsForBluetooth = arrayOf<String>(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }
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
        checkPermissions(permissionsForBluetooth)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        myBluetoothAdapter = bluetoothManager.adapter
            //BluetoothAdapter.getDefaultAdapter()
        if(myBluetoothAdapter == null) {
            Toast.makeText(this, "该设备不支持蓝牙", Toast.LENGTH_SHORT).show()
            isSupportBlue = false
        }
        //如果支持蓝牙，但是蓝牙没有打开，请求打开蓝牙
        //通过registerForActivityResult获取结果
        if(isSupportBlue&&!myBluetoothAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,"请先开放蓝牙权限！",Toast.LENGTH_SHORT).show()
                return
            }
            val startActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                //此处进行数据接收（接收回调）
                if (it.resultCode == Activity.RESULT_OK) {
                    if (myBluetoothAdapter!!.isEnabled) {
                        //同意开启了蓝牙设备
                        Toast.makeText(this, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        //没有同意开启蓝牙设备
                        Toast.makeText(this, "Bluetooth has been disabled", Toast.LENGTH_SHORT).show()
                    }
                } else if (it.resultCode == Activity.RESULT_CANCELED) {
                    //没有同意开启蓝牙设备
                    Toast.makeText(this, "Bluetooth enabling has been canceled", Toast.LENGTH_SHORT).show()
                }

            }
            startActivity.launch(enableBluetoothIntent)
        }

        binding.btnRefreshBlueDevice.setOnClickListener {
            pairedDeviceList()
        }
    }

    //获取已经配对的蓝牙列表
    private fun pairedDeviceList() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,"请先开放蓝牙权限！",Toast.LENGTH_SHORT).show()
            return
        }
        myPairedDevices = myBluetoothAdapter!!.bondedDevices
        //   val list : ArrayList<BluetoothDevice> = ArrayList()
        val list : ArrayList<BlueDevice> = ArrayList()
        if (myPairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in myPairedDevices) {
                list.add(BlueDevice(device.name,device))
                Log.i("device", ""+device)
            }
        } else {
            Toast.makeText(this, "没有找到蓝牙设备", Toast.LENGTH_SHORT).show()
        }
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerBlueDeviceList.layoutManager = layoutManager
        val adapter = BlueDeviceListAdapter(list,this)
        binding.recyclerBlueDeviceList.adapter = adapter

    }

    //创建实体类,存放蓝牙名和蓝牙地址
    class BlueDevice(val deviceName:String,val device:BluetoothDevice)

    private fun checkPermissions(permissionArrays: Array<String>) : Boolean {
        var allGranted = false
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d("BlueToothActivity", "onDestroy")
    }
}