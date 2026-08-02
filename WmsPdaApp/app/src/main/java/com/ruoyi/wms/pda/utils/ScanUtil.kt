package com.ruoyi.wms.pda.utils

import android.app.Activity
import android.content.Intent
import com.journeyapps.barcodescanner.CaptureActivity

/**
 * 摄像头扫码工具，通过Intent启动CaptureActivity。
 * 在Activity中调用:
 *   ScanUtil.startScan(this)
 * 然后在onActivityResult中处理:
 *   if (requestCode == ScanUtil.REQUEST_CODE) { ... }
 */
object ScanUtil {
    const val REQUEST_CODE = 0x1001

    fun startScan(activity: Activity) {
        val intent = Intent(activity, CaptureActivity::class.java)
        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    /**
     * 在onActivityResult中调用，解析扫码结果。
     * @return 扫码内容，取消返回null
     */
    fun parseResult(requestCode: Int, resultCode: Int, data: Intent?): String? {
        if (requestCode != REQUEST_CODE) return null
        if (resultCode != Activity.RESULT_OK) return null
        return data?.getStringExtra("SCAN_RESULT")
    }
}
