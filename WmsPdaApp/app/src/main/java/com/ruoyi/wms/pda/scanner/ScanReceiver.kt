package com.ruoyi.wms.pda.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * PDA硬件扫码接收器。
 * 支持多种PDA品牌的扫码广播：
 * - Honeywell/海讯: com.honeywell.scan.intent / data extra
 * - Zebra/Motorola: com.symbol.scan / com.motorolasolutions.emdk.datawedge.DATA_STRING
 * - 新大陆/PT380: android.intent.action.SCANRESULT / scan_data
 * - 通用: com.android.server.scannerservice.broadcast / scannerdata
 */
class ScanReceiver(
    private val onScan: (String) -> Unit
) : BroadcastReceiver() {
    companion object {
        // Common PDA scan action names
        private val SCAN_ACTIONS = arrayOf(
            "com.honeywell.scan.intent",
            "com.symbol.scan",
            "com.motorolasolutions.emdk.datawedge.ACTION_SCANNER",
            "android.intent.action.SCANRESULT",
            "com.android.server.scannerservice.broadcast",
            "com.scanner.broadcast",
            "scan.rcv.message"
        )
        // Common data extra keys
        private val SCAN_DATA_KEYS = arrayOf(
            "data",
            "scan_data",
            "scannerdata",
            "scannerdata_str",
            "barcode_string",
            "decode_data",
            "barcode"
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent ?: return
        // Try each known extra key
        for (key in SCAN_DATA_KEYS) {
            val data = intent.getStringExtra(key)
            if (!data.isNullOrEmpty()) {
                onScan(data.trim())
                return
            }
        }
    }

    /**
     * Create IntentFilter matching all known PDA scan actions.
     */
    fun createIntentFilter(): IntentFilter {
        val filter = IntentFilter()
        for (action in SCAN_ACTIONS) {
            filter.addAction(action)
        }
        return filter
    }
}
