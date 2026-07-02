# GuitarTuna

Android 吉他调音器，标准六弦调音（E A D G B E）。

## 构建

```bash
ANDROID_HOME=/home/rd/Android/Sdk ./gradlew assembleDebug
```

## 安装

```bash
# 连接手机后
./gradlew installDebug
# 或手动安装
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- AudioRecord 低延迟音频采集
- 自相关算法（Hann 窗 + 抛物线插值）音高检测

## 功能

- 手动选弦 / 自动检测模式
- 实时音分偏差显示 + 红黄绿渐变调音表
- ±2 音分内锁定显示"已调准"
