#!/bin/bash
# 在真机上运行 EcoGo Android 应用的便捷脚本

echo "🔍 检查连接的设备..."
ADB="$HOME/Library/Android/sdk/platform-tools/adb"

# 检查设备连接
DEVICES=$($ADB devices | grep -v "List of devices" | grep "device$" | wc -l)

if [[ "$DEVICES" -eq 0 ]]; then
    echo "❌ 未检测到设备！请确保："
    echo "   1. 手机通过 USB 连接到电脑"
    echo "   2. 手机已开启 USB 调试"
    echo "   3. 手机上已授权此电脑进行调试"
    exit 1
fi

echo "✅ 检测到 $DEVICES 个设备"
$ADB devices

echo ""
echo "📦 开始构建应用..."
./gradlew clean assembleDebug

if [[ $? -ne 0 ]]; then
    echo "❌ 构建失败！请检查错误信息"
    exit 1
fi

echo ""
echo "📲 正在安装应用到设备..."
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

if [[ $? -ne 0 ]]; then
    echo "❌ 安装失败！"
    exit 1
fi

echo ""
echo "🚀 正在启动应用..."
$ADB shell am start -n com.ecogo/.MainActivity

echo ""
echo "✅ 完成！EcoGo 应用应该已经在你的手机上启动了"
echo ""
echo "💡 提示："
echo "   - 查看日志: ./gradlew installDebug && $ADB logcat | grep EcoGo"
echo "   - 卸载应用: $ADB uninstall com.ecogo"
echo "   - 重启应用: $ADB shell am start -n com.ecogo/.MainActivity"
