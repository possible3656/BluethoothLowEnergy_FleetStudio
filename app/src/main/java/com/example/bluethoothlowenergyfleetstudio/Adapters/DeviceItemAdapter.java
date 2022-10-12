package com.example.bluethoothlowenergyfleetstudio.Adapters;

import android.Manifest;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluethoothlowenergyfleetstudio.R;

import java.util.ArrayList;

public class DeviceItemAdapter extends RecyclerView.Adapter<DeviceItemAdapter.ViewHolder> {
    ArrayList<BluetoothDevice> bluetoothDeviceArrayList;
    Context context;
    boolean isPairedDevice;
    private OnItemClicked onClick;

    public interface OnItemClicked {
        void onItemClick(int position,boolean isPairedDevice);
    }

    public DeviceItemAdapter(ArrayList<BluetoothDevice> bluetoothDeviceArrayList, Context context, boolean isPairedDevice, OnItemClicked onClick) {
        this.bluetoothDeviceArrayList = bluetoothDeviceArrayList;
        this.context = context;
        this.isPairedDevice = isPairedDevice;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public DeviceItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.device_item_view, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(contactView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceItemAdapter.ViewHolder holder, int position) {
        BluetoothDevice device = bluetoothDeviceArrayList.get(position);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            holder.textView.setText(device.getName());
            if (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
                holder.imageView.setImageResource(R.drawable.ic_baseline_headphones_24);
            }
//            Log.d("deviceType", "onBindViewHolder: " + device.getBluetoothClass().getDeviceClass());
        }
        holder.itemView.setOnClickListener(view -> onClick.onItemClick(position,isPairedDevice));


    }

    @Override
    public int getItemCount() {
        return bluetoothDeviceArrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.device_image_view);
            textView = itemView.findViewById(R.id.device_name);


        }
    }


}
