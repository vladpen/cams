## Cams

![Cams](https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/5_cover.jpg)

Простое мобильное приложение под Android для воспроизведения RTSP потоков с IP камер.

Особенности:

- Просмотр RTSP потоков c любых IP камер, включая H.265+.
- Одновременный просмотр нескольких потоков.
- Двадцатикратное увеличение изображения.
- Просмотр видеозаписей по протоколу SFTP.
- Максимальная скорость подключения.
- Возможность переключения протокола TCP/UDP.
  Эта опция важна при просмотре камер через интернет, где UDP может не поддерживаться или работать плохо.
- Предельная простота навигации и управления.

<img src="https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/1_main_ru.jpg"
alt="Main screen"
width="200">&nbsp;
<img src="https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/2_edit_ru.jpg"
alt="Edit screen"
width="200">&nbsp;
<img src="https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/3_files_ru.jpg"
alt="Files screen"
width="200">&nbsp;
<img src="https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/4_video_ru.jpg"
alt="Video screen"
width="200">

Приложение написано для совместного использования с сервером [python-rtsp-server](https://github.com/vladpen/python-rtsp-server),
но прекрасно работает автономно благодаря возможности подключения к любым IP камерам, а также видеорегистраторам, поддерживающим SFTP.

Воспроизводит большинство типов видеопотоков (не только RTSP).
На снимке экрана выше показано изображение с реальной видеокамеры и три тестовых ролика в режиме "Группа".

## Установка

APK файл можно собрать самостоятельно, скачать с [Github](https://github.com/vladpen/cams/tree/main/app/release)
или [F-Droid](https://f-droid.org/ru/packages/com.vladpen.cams/).
Поддерживается архитектура ARM-64 (используется в большинстве современных мобильных телефонов), ARM, x86-64 и x86.

## Настройка

Для подключения к видеокамере нужно ввести в поле "Адрес" ее URL, указанный производителем. Обычно он выглядит так:
```
rtsp://[<пользователь>:<пароль>@]<IP>:<порт>/<путь>
```
Параметры в квадратных скобках необязательны.

Адрес SFTP сервера или видеорегистратора выглядит так:
```
sftp://<пользователь>:<пароль>@<IP>[:<порт>]/<путь>
```
ВНИМАНИЕ! Настоятельно не рекомендуется использовать данные доступа администратора.
Для SFTP сервера лучше создать chroot, например, как описано [тут](https://wiki.archlinux.org/title/SFTP_chroot).

Подробное обсуждение приложения: [habr.com/ru/post/654915](https://habr.com/ru/post/654915/)
и сервера: [habr.com/ru/post/597363](https://habr.com/ru/post/597363/).

[<img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
alt="Get it on Github"
height="80">](https://github.com/vladpen/cams/tree/main/app/release/)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/com.vladpen.cams/)
[<img src="https://store.nashstore.ru/assets/images/logo.svg"
alt="Get it on NashStore"
height="80">](https://store.nashstore.ru/store/628380ec4891a5de4cd8d26f)

*Copyright (c) 2022 vladpen under MIT license. Use it with absolutely no warranty.*