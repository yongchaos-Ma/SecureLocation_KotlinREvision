package com.example.corekotlinremaster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

//创建RecyclerView适配器
class BlueDeviceListAdapter(val deviceList: List<BluetoothActivity.BlueDevice>, val context: Context): RecyclerView.Adapter<BlueDeviceListAdapter.ViewHolder>(){
    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val deviceName: TextView = view.findViewById(R.id.item_name)
        val deviceAddress: TextView = view.findViewById(R.id.address_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_blue_item,parent,false)
        val viewHolder = ViewHolder(view)

        //点击事件,启动蓝牙收发Activity
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.adapterPosition
            val device = deviceList[position]
            val intent = Intent(context,MainActivity::class.java)
            Log.d("ListAdapter", "address: ${device.device.address}; name: ${device.deviceName}")
            intent.putExtra(BluetoothActivity.BLUE_ADDRESS, device.device.address)
            intent.putExtra(BluetoothActivity.BLUE_NAME,device.deviceName)
            context.startActivity(intent)
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = deviceList[position]
        holder.deviceName.text = device.deviceName
        holder.deviceAddress.text = device.device.address
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}