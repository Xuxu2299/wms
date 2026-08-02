package com.ruoyi.wms.pda.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.ruoyi.wms.pda.R
import com.ruoyi.wms.pda.data.api.ApiClient
import com.ruoyi.wms.pda.data.api.GetInfoResponse
import com.ruoyi.wms.pda.data.api.LoginRequest
import com.ruoyi.wms.pda.data.api.LoginResponse
import com.ruoyi.wms.pda.data.prefs.SessionManager
import com.ruoyi.wms.pda.ui.base.BaseActivity
import com.ruoyi.wms.pda.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 登录页。
 *
 * - 服务器地址默认取 SessionManager.serverUrl，为空时回退到 http://192.168.1.100:8080/
 * - PDA 简化：跳过验证码，login 时传空 code/uuid
 * - 登录成功后保存 token，并调用 getInfo() 获取用户详情（userId）
 */
class LoginActivity : BaseActivity() {

    private lateinit var etServerUrl: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    companion object {
        private const val DEFAULT_SERVER_URL = "http://192.168.1.100:8080/"
    }

    /** 登录页无需硬件扫码 */
    override val enableHardwareScanner: Boolean
        get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etServerUrl = findViewById(R.id.etServerUrl)
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        // 预填服务器地址
        val savedUrl = SessionManager.serverUrl
        etServerUrl.setText(if (savedUrl.isNotEmpty()) savedUrl else DEFAULT_SERVER_URL)

        // 已登录则直接进入主界面
        if (SessionManager.isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        btnLogin.setOnClickListener { doLogin() }
    }

    private fun doLogin() {
        val serverUrl = etServerUrl.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (TextUtils.isEmpty(serverUrl)) {
            toast("请输入服务器地址"); return
        }
        if (TextUtils.isEmpty(username)) {
            toast("请输入用户名"); return
        }
        if (TextUtils.isEmpty(password)) {
            toast("请输入密码"); return
        }

        // 保存服务器地址并重建 Retrofit，使新地址生效
        SessionManager.serverUrl = serverUrl
        ApiClient.recreate()

        lifecycleScope.launch {
            try {
                showLoading("登录中...")
                // PDA 简化：跳过验证码，code/uuid 传空
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.api.login(LoginRequest(username, password, "", "")).execute()
                }
                if (!resp.isSuccessful) {
                    toastLong("登录失败: HTTP ${resp.code()}")
                    return@launch
                }
                val body: LoginResponse? = resp.body()
                val token = body?.token
                if (body == null || !body.isSuccess || token.isNullOrEmpty()) {
                    toastLong(body?.msg ?: "登录失败")
                    return@launch
                }

                // 保存登录信息
                SessionManager.token = token
                SessionManager.username = username

                // 获取用户详情
                fetchUserInfo()

                toast("登录成功")
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                toastLong("登录异常: ${e.message}")
            } finally {
                hideLoading()
            }
        }
    }

    /** 调用 getInfo() 获取用户 ID 等信息并保存到 SessionManager */
    private suspend fun fetchUserInfo() {
        try {
            val infoResp = withContext(Dispatchers.IO) {
                ApiClient.api.getInfo().execute()
            }
            val info: GetInfoResponse? = infoResp.body()
            if (infoResp.isSuccessful && info != null && info.isSuccess) {
                info.user?.userId?.let { SessionManager.userId = it }
                info.user?.userName?.let { SessionManager.username = it }
            }
        } catch (_: Exception) {
            // 获取用户信息失败不阻断登录流程
        }
    }
}
