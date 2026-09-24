# Android 数据集 APK 清单（APK_MANIFEST）

本目录存放 BlackBoxBench Android 数据集 26 个目标的原始 APK（未修改的上游
构建），用于基准测试的可复现性。**用途、版权与许可证声明见 [NOTICE.md](NOTICE.md)**。

- 完整性校验：`SHA256SUMS.txt`（`joplin.apk` 因超过 GitHub 单文件 100MB 限制
  拆为 `part-aa` / `part-ab` 两个分卷，合并命令见该文件注释）。
- 恢复到运行位置：`vendor/python/python.exe scripts/restore_android_apks.py`
  （复制到 `runs/android_targets/artifacts/<app_id>/<sha256>.apk` 并校验）。
- **许可证以上游仓库为准**；标注"见上游"的条目请在对应链接核对。

| app_id | 包名 | 上游 | 许可证 | 大小 |
|---|---|---|---|---:|
| ankidroid | com.ichi2.anki | https://github.com/ankidroids/Anki-Android | GPL-3.0 | 39.5 MB |
| antennapod | de.danoeh.antennapod | https://github.com/AntennaPod/AntennaPod | GPL-3.0 | 16.9 MB |
| cashew | com.budget.tracker_app | https://github.com/jameskokoska/Cashew | 见上游 | 40.5 MB |
| feeder | com.nononsenseapps.feeder | https://github.com/spacecowboy/Feeder | GPL-3.0 | 52.0 MB |
| fossify_calculator | org.fossify.math | https://github.com/FossifyOrg/Calculator | GPL-3.0 | 6.7 MB |
| fossify_calendar | org.fossify.calendar | https://github.com/FossifyOrg/Calendar | GPL-3.0 | 7.4 MB |
| fossify_gallery | org.fossify.gallery | https://github.com/FossifyOrg/Gallery | GPL-3.0 | 36.8 MB |
| fossify_paint | org.fossify.paint | https://github.com/FossifyOrg/Paint | GPL-3.0 | 7.3 MB |
| futoshiki | com.hexcorp.futoshiki | https://f-droid.org/packages/com.hexcorp.futoshiki/ | 见上游 | 10.2 MB |
| game2048 | org.andstatus.game2048 | https://github.com/andstatus/game2048 | 见上游 | 1.3 MB |
| google_clock | com.google.android.deskclock | 设备预装（无公开源码），见 NOTICE 专有条款 | **专有（Google）** | 13.7 MB |
| joplin | net.cozic.joplin | https://github.com/laurent22/joplin | AGPL-3.0 | 144.4 MB¹ |
| librera | com.foobnix.pro.pdf.reader | https://github.com/foobnix/LibreraReader | GPL-3.0 | 94.5 MB |
| loop_habit_tracker | org.isoron.uhabits | https://github.com/iSoron/uhabits | GPL-3.0 | 10.2 MB |
| markor | net.gsantner.markor | https://github.com/gsantner/markor | 见上游 | 11.5 MB |
| material_files | me.zhanghai.android.files | https://github.com/zhanghai/MaterialFiles | GPL-3.0 | 11.6 MB |
| metronome | com.bobek.metronome | https://f-droid.org/packages/com.bobek.metronome/ | 见上游 | 3.0 MB |
| minesweeper | io.github.johnathan.minesweeper | https://f-droid.org/packages/io.github.johnathan.minesweeper/ | 见上游 | 0.9 MB |
| mj_pdf | com.gitlab.mudlej.MjPdfReader | https://gitlab.com/mudlej/MjPdfReader | 见上游 | 8.7 MB |
| nonogram | com.sidhant.nonogram | https://f-droid.org/packages/com.sidhant.nonogram/ | 见上游 | 8.2 MB |
| organic_maps | app.organicmaps.web | https://github.com/organicmaps/organicmaps | Apache-2.0 | 61.2 MB |
| snapseed | com.niksoftware.snapseed | Google Play（Nik Software / Google），见 NOTICE 专有条款 | **专有（Google）** | 28.2 MB |
| tasks | org.tasks | https://github.com/tasks/tasks | GPL-3.0 | 26.1 MB |
| ultimate_ttt | org.kirkezz.rttt | https://github.com/kirkezz | 见上游 | 22.3 MB |
| vinyl | com.poupa.vinylmusicplayer | https://github.com/AdrienPoupa/VinylMusicPlayer | GPL-3.0 | 7.9 MB |
| vlc | org.videolan.vlc | https://code.videolan.org/videolan/vlc-android | GPL-2.0+（另遵 VideoLAN 商标/分发政策） | 47.2 MB |

¹ `joplin.apk` 以 `joplin.apk.part-aa` + `joplin.apk.part-ab` 分卷存放（GitHub
单文件 100MB 限制），合并后为 144.4 MB，sha256 见 `SHA256SUMS.txt`。

## GPL 系应用合规说明

本目录分发的均为**未经修改的上游二进制**（字节级一致，见 SHA256SUMS）。GPL/AGPL
应用对应的**完整对应源码**可在上表上游链接获取；如需本仓库提供书面源码承诺，
请开 issue 说明。

## 专有应用（google_clock / snapseed）说明

二者无开源许可证，版权归 Google LLC / Nik Software。副本仅作学术评测对照目标
（详见 NOTICE.md 第 6 条与"许可证合规"节），使用本仓库即视为已知悉该状态；
如需规避，可从自有设备提取同包名 APK 后按 `SHA256SUMS.txt` 校验替换。
