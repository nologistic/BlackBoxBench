# Vinyl Music Player 复现补充素材

本目录是 `vinyl`（本地音乐播放器）复现工作区的目标专属素材包，挂载为
只读的 `/materials/app`。所有艺人与曲目均为虚构。

## 实体素材

- `music_library.json`：虚构曲库（4 位艺人、6 张专辑、24 首曲目，
  含时长/曲目号/年份/流派），是音乐库功能的**必要数据**。
- `playlists.m3u`：两份 M3U 播放列表样例（晨间通勤 / 深夜编码）。

## 非实体补充信息

见 `SUPPLEMENT.md`：曲库聚合模型（艺人/专辑/流派/年份）、播放队列与
shuffle/repeat 语义、黑胶动画与睡眠定时。

## 使用规则

不要修改本目录。需要使用时将文件复制进 `/workspace` 工程内再引用；
播放音频复用 `/materials/mobile/audio/` 的 WAV 样本（按曲目轮转映射）。
