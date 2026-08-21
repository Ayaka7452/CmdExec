# CmdExec（Command Executor）

一个轻量 Android 应用：在应用内为 4 个命令槽位填写名称与命令，并独立选择执行模式（Root / Shell）。保存后可通过桌面长按图标菜单快速运行，也可在“日常程序”中添加应用快捷方式调用。

## 功能

- 4 个命令槽位，每个可填写名称（用于菜单显示）、命令，并独立选择执行模式。
- 输入内容自动保存（本地存储）；每行可单独运行测试并查看输出与退出码。
- 每行提供“清空”按钮，清空前需确认。
- 桌面长按应用图标出现 4 个菜单项，显示自定义名称；点击后在后台直接执行，不弹出界面。
- Root 模式通过 `su -c` 执行，兼容 KernelSU / Magisk；Shell 模式通过 `sh -c` 以当前权限执行。

## 使用

1. 安装 APK。
2. 打开应用，为各槽位填写名称（可选）和命令，并选择执行模式，例如：
   - `settings put system screen_brightness 100`
   - `cmd connectivity airplane-mode enable`
   - `svc wifi disable`
3. 返回桌面，长按应用图标 → 点击对应的菜单项即可运行。
4. 首次使用 Root 模式时，系统会请求超级用户授权。
5. 也可在“日常程序”中添加应用快捷方式，以调用这些命令。

## 说明

- 名称、命令和执行模式均保存在应用私有存储中，不会上传。
- 命令为单行 shell 命令，可使用 `&&`、`;` 等组合。
- 执行超时时间为 60 秒，超时会强制终止进程。
- 桌面应用名可在 `app/src/main/res/values/strings.xml` 的 `app_name` 中修改。

## 编译

推送到本仓库的 `main` 分支后，GitHub Actions 会自动执行 `./gradlew assembleDebug`，编译产物可在 Actions 页面下载。
