### 人人影视视频下载

主要代码来自反编译APK, 只研究讨论下载这个过程，如果作者介意请告知，我会立即删除整个项目。

平时不常用Android，iOS不太好装这个，迅雷又偶尔封链接。。。一看人人直接用GET方法下载，准备写个脚本。写完发现下载下来的文件打不开，用Chrome下完的一瞬间文件会大很多，网上查资料看到这篇讨论 [https://github.com/soimort/you-get/issues/2286](https://github.com/soimort/you-get/issues/2286)。好奇心起，反编译了代码拷出来跑了一下，原来是每8KB加密了，一边下载一边解密，奇怪的是试了五次波杰克，四次文件不完整一次没问题，文件大小是一样的，播放到某个时间点后面的就都看不了了，可是代码没有报任何Exception，这里也没有断点续传的代码。奇怪奇怪，有空再研究。