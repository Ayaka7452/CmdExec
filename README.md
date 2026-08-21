# Root 快捷命令（CmdExec）

一个极简的 Android 应用：在应用里填写 4 条 root 命令，保存后即可通过**桌面长按图标菜单**直接以 root 执行，也可以被三星日常程序（Bixby Routines）通过“应用快捷方式”调用。

## 功能

- 主界面 4 个命令输入框，输入内容自动保存（本地存储）。
- 每行一个“运行”按钮，可单独测试某条命令并查看输出与退出码。
- 桌面长按应用图标，出现“命令 1 ~ 4”四个菜单项，点击即执行对应命令。
- 从桌面菜单触发时完全在后台执行，不弹出任何界面；查看输出和退出码请在应用内用“运行”按钮测试。
- 通过 `su -c` 执行，兼容 KernelSU / Magisk。

## 使用

1. 安装 APK（GitHub Actions 编译产物 `app-debug.apk`，或 Android Studio 直接运行）。
2. 打开应用，在四个输入框中填写命令，例如：
   - `settings put system screen_brightness 100`
   - `cmd connectivity airplane-mode enable`
   - `svc wifi disable`
3. 返回桌面，长按应用图标 → 点击对应的“命令 N”即可执行。
4. 首次执行时 KernelSU / Magisk 会弹出 root 授权请求，选择允许并勾选记住选择。
5. 三星日常程序：新建例程 → “然后” → 添加“应用快捷方式” → 选择本应用和对应命令项。

## 说明

- 命令保存在应用私有存储中，不会上传。
- 命令为单行 shell 命令，可自行组合 `&&`、`;` 等（由 `su -c` 交给 shell 解释）。
- 执行超时时间为 60 秒，超时会强制终止进程。
- 修改 `app/src/main/res/values/strings.xml` 中的 `app_name` 可更改桌面显示名称。

## 编译

推送到本仓库的 `main` 分支后，GitHub Actions 会自动执行 `./gradlew assembleDebug`，编译产物可在 Actions 页面下载。
