# VLC 复现补充素材

本目录是 `vlc`（多媒体播放器）复现工作区的目标专属素材包，挂载为
只读的 `/materials/app`。所有内容均为虚构测试数据。

## 实体素材

- `subtitle_sample.srt`：SRT 字幕样例（中英双语段落），用于外挂字幕
  加载功能。
- `playlists.m3u`：两份 M3U 播放列表样例。

## 非实体补充信息

见 `SUPPLEMENT.md`：媒体库分组、播放器手势、字幕与音轨、均衡器与
播放速度、播放列表语义。

## 使用规则

不要修改本目录。需要使用时将文件复制进 `/workspace` 工程内再引用；
播放媒体复用 `/materials/mobile/video/` 的 MP4 与 `/materials/mobile/audio/`
的 WAV 样本。
