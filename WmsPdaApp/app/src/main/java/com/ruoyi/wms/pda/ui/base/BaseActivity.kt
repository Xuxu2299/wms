package com.ruoyi.wms.pda.ui.base

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.ruoyi.wms.pda.scanner.ScanReceiver

/**
 * Activity基类，提供：
 * - Toast快捷方法
 * - Loading对话框
 * - PDA硬件扫码注册/注销
 * - 扫码回调
 *
 * 注意：基类继承 androidx.activity.ComponentActivity 而非 AppCompatActivity。
 * 因主题已改为框架主题 @android:style/Theme.NoTitleBar（非 AppCompat 主题），
 * AppCompatActivity 会因主题校验而崩溃；ComponentActivity 同样提供 lifecycleScope
 * 等能力，但不强制要求 AppCompat 主题。
 */
abstract class BaseActivity : ComponentActivity() {
    private var loadingDialog: ProgressDialog? = null
    private var scanReceiver: ScanReceiver? = null

    /** 子类实现：收到扫码数据后的回调 */
    protected open fun onScanResult(code: String) {}

    /** 是否启用硬件扫码（默认true，子类可覆盖关闭） */
    protected open val enableHardwareScanner: Boolean get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (enableHardwareScanner) {
            registerScanner()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterScanner()
    }

    private fun registerScanner() {
        scanReceiver = ScanReceiver { code ->
            runOnUiThread { onScanResult(code) }
        }
        registerReceiver(scanReceiver, scanReceiver!!.createIntentFilter())
    }

    private fun unregisterScanner() {
        scanReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        scanReceiver = null
    }

    protected fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    protected fun toastLong(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    protected fun showLoading(msg: String = "加载中...") {
        hideLoading()
        loadingDialog = ProgressDialog(this).apply {
            setMessage(msg)
            setCancelable(false)
            show()
        }
    }

    protected fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}
