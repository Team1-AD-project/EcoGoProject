package com.ecogo.api

/**
 * API 配置
 * 后端服务器配置
 */
object ApiConfig {
    private fun isEmulator(): Boolean {
        // Common emulator fingerprints/props (avoid network guesswork).
        val fp = android.os.Build.FINGERPRINT ?: ""
        val model = android.os.Build.MODEL ?: ""
        val brand = android.os.Build.BRAND ?: ""
        val device = android.os.Build.DEVICE ?: ""
        val product = android.os.Build.PRODUCT ?: ""
        return fp.contains("generic", ignoreCase = true) ||
            fp.contains("emulator", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("Android SDK built for", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product.contains("sdk", ignoreCase = true)
    }

    /**
     * 基础 URL
     * 
     * 使用说明：
     * - 模拟器访问本机: http://10.0.2.2:8090/
     * - 真实设备访问本机: http://192.168.x.x:8090/ (替换为你的电脑IP)
     * - 生产环境: https://your-domain.com/
     */
    val BASE_URL: String by lazy {
        // If we're on emulator, always use the special host alias 10.0.2.2 to reach the PC.
        // This prevents common timeouts when a LAN IP is configured by mistake.
        if (isEmulator()) "http://10.0.2.2:8090/" else com.ecogo.BuildConfig.ECOGO_BASE_URL
    }
    
    /**
     * API 版本
     */
    const val API_VERSION = "v1"
    
    /**
     * 完整的 API 路径
     */
    const val API_PATH = "api/$API_VERSION/"
    
    /**
     * 连接超时时间（秒）
     * 优化：从30秒减少到10秒，避免长时间等待
     */
    const val CONNECT_TIMEOUT = 10L
    
    /**
     * 读取超时时间（秒）
     * 优化：从30秒减少到15秒
     */
    const val READ_TIMEOUT = 15L
    
    /**
     * 写入超时时间（秒）
     * 优化：从30秒减少到15秒
     */
    const val WRITE_TIMEOUT = 15L
}
