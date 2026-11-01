# 构建与检查

## 标准 Gradle 构建

需要 Java 17+、Android SDK 35、Build Tools 35.0.0，以及 Node.js 22+（测试使用）。仓库包含固定为 Gradle 8.11.1 的 Wrapper，并校验官方发行包 SHA-256。

```sh
# Windows
gradlew.bat :app:assembleDebug

# macOS / Linux
bash ./gradlew :app:assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`。Android Studio 可直接打开本仓库。

将本机 SDK 路径写入不提交的 `local.properties`，或设置 `ANDROID_HOME`；设置 `JAVA_HOME` 指向 JDK。不要把个人路径提交到仓库。

## 测试

```sh
npm test
```

完整检查（PowerShell 7，Windows/macOS/Linux）：

```powershell
./tools/test.ps1
# 也可显式指定 JDK
./tools/test.ps1 -Jdk '/path/to/jdk'
```

当前包含 24 项 JavaScript 测试、40 项导航断言、27 项课表/提醒断言。GitHub Actions 另外构建 Android APK，并提供短期 CI 构建产物。

## Windows 最小构建

没有安装 Gradle 构建依赖时，可使用本机已有的 Android 35 `android.jar` 与 JDK。此脚本使用 AAPT2/R8/apksig，不会修改系统配置。

```powershell
./tools/download-tools.ps1
./tools/build.ps1 -Jdk 'C:/path/to/jdk' -AndroidJar 'C:/path/to/sdk/platforms/android-35/android.jar'
```

也支持 `JAVA_HOME` 与 `ANDROID_HOME` / `ANDROID_SDK_ROOT` 环境变量。输出：`output/qingke-0.4.1-debug.apk`。应用 ID、版本号和 SDK 版本统一从 `app/build.gradle` 读取。

下载脚本会校验 Google Maven 工具的 SHA-256。手工构建的测试签名由本机生成，文件位于 `tools/qingke-debug.p12`，已加入忽略规则，不能提交。

## 签名和升级

GitHub CI 的默认 debug APK 使用临时构建环境的签名，可能无法覆盖安装 Releases 中的 APK。普通用户请下载 Releases 附件。

目前 Release 是开发体验签名，并与此前提供的体验版保持一致。该私钥仅保存在维护者本机，不进入源码或 CI。若自行构建，使用的签名可能不同；换包前先备份数据。正式发行时应单独配置并保管正式签名。

签名代码中的 `android` 是开发测试密码，不是学校账号凭据，也不是可用于访问服务的令牌。
