package com.kubeapp.flutterkube.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kubeapp.flutterkube.service.KubeService

class ReceiverBluetooth(private val service: KubeService) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> pausePlayer()
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_TURNING_OFF) {
                    pausePlayer()
                }
            }
        }
    }

    private fun pausePlayer() {
        service.player?.pause()
    }
}