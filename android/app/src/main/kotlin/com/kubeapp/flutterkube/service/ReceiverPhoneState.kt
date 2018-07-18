package com.kubeapp.flutterkube.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_NEW_OUTGOING_CALL
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED

class ReceiverPhoneState : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_NEW_OUTGOING_CALL -> pausePlayer(context)
            ACTION_PHONE_STATE_CHANGED -> {
                when (intent.extras?.getString(TelephonyManager.EXTRA_STATE)) {
                    TelephonyManager.EXTRA_CALL_VOICEMAIL_INTENT,
                    TelephonyManager.EXTRA_STATE_RINGING -> pausePlayer(context)
                }
            }
        }
    }

    private fun pausePlayer(context: Context?) {
        val serviceIntent = Intent(context, KubeService::class.java)
        serviceIntent.action = KubeService.ACTION_PAUSE
        context?.startService(serviceIntent)
    }

}