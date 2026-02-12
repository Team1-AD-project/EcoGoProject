package com.ecogo.app

import android.app.Application
import com.ecogo.app.data.repository.NavigationHistoryRepository

/**
 * EcoGo应用程序类
 * 用于全局初始化
 */
class EcoGoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化导航历史记录仓库
        NavigationHistoryRepository.initialize(this)
    }
}
