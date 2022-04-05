## Cams

![Cams](https://raw.githubusercontent.com/vladpen/cams/screenshots/img/cover.png)

Простое мобильное приложение под Android для воспроизведения RTSP потоков с IP камер.

Особенности:

- Просмотр RTSP потоков c любых IP самер, включая H.265+.
- Возможность переключения протокола TCP/UDP.
  Эта опция важна при просмотре камер через интернет, где UDP может не поддерживаться или работать плохо.
- Просмотр видеозаписей по протоколу SFTP.
- Двадцатикратное увеличение изображения.
- Максимальная скорость подключения.
- Предельная простота навигации и управления.

![Screenshots](https://raw.githubusercontent.com/vladpen/cams/screenshots/img/screens.png)

Приложение написано специально для работы с сервером [python-rtsp-server](https://github.com/vladpen/python-rtsp-server), 
но прекрасно работает автономно благодаря возможности подключаться к любым RTSP потокам и видеорегистраторам,
поддерживающим SFTP.

Подробное обсуждение: [habr.com/ru/post/654915](https://habr.com/ru/post/654915/)

## Установка

APK файл можно собрать самостоятельно, скачать с [Github](https://github.com/vladpen/cams/tree/main/app/release)
или [F-Droid](https://f-droid.org/ru/packages/com.vladpen.cams/).
Поддерживаются архитектуры ARM-64 (используется в большинстве современных мобильных телефонов), ARM, x86-64 и x86.

*Copyright (c) 2022 vladpen under MIT license. Use it with absolutely no warranty.*
