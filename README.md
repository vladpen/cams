## Cams

![Cams](https://raw.githubusercontent.com/vladpen/cams/main/fastlane/metadata/android/ru-RU/images/phoneScreenshots/5_cover.jpg)

Простое мобильное приложение под Android для воспроизведения RTSP потоков с IP камер.

Особенности:

- Просмотр RTSP потоков c любых IP камер, включая H.265+.
- Одновременный просмотр нескольких потоков.
- Двадцатикратное увеличение изображения.
- Поддержка двухканальных камер.
- Просмотр видеозаписей или изображений по протоколу SFTP.
- Возможность настройки оповещений о срабатывании детектора движения камеры.
- Высокая скорость подключения.
- Предельная простота навигации и управления.
- Максимальная безопасность и конфиденциальность данных.
- Переключение протокола TCP/UDP.
  Эта опция важна при просмотре камер через интернет, где UDP может не поддерживаться или работать плохо.

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

*ВАЖНО. Приложение ориентировано на безопасность и приватность данных, поэтому не собирает и не обрабатывает никакую информацию о пользователе.
Данные не отправляются ни на какие сервера, включая техническую инфраструктуру Google и "облачные" хранилища производителей камер.*

## Установка

APK файл можно собрать самостоятельно, [скачать с Github](https://github.com/vladpen/cams/raw/main/app/release/app-armeabi-v7a-release.apk),
установить с помощью [F-Droid](https://f-droid.org/ru/packages/com.vladpen.cams/) или [RuStore](https://apps.rustore.ru/app/com.vladpen.cams).
Поддерживается архитектура ARM-64 (используется в большинстве современных мобильных телефонов), ARM, x86-64 и x86.

## Настройка

Для подключения к видеокамере нужно ввести в поле "Адрес" ее URL, указанный производителем. Обычно он выглядит так:
```
[rtsp://][<пользователь>:<пароль>@]<IP>[:<порт>][/<путь>]
```
Параметры в квадратных скобках необязательны (зависит от настроек камеры).

Для двухканальных камер дополнительно можно указать адрес второго канала.
Нпример, для камер Hikvision и их производных путь будет иметь такой вид:
```
ISAPI/Streaming/Channels/<номер канала>
```
Тогда первый канал (высокого разрешения) будет иметь номер 101, а второй (низкого разрешения) — 102.

Каналы низкого разрешения можно использовать для ускорения загрузки изображения,
для экономии трафика и для снижения нагрузки на процессор устройства.
Это особенно удобно для просмотра группы камер при низкой скорости соединения. 
При воспроизведении каналы можно переключать кнопкой К1/К2 в нижнем правом углу экрана.
На экранах групп камер по умолчанию используется K2.

Также для снижения нагрузки воспроизведение камер, выходящих за границы экрана при увеличении изображения, приостанавливается.

Адрес SFTP сервера или видеорегистратора выглядит так:
```
[sftp://]<пользователь>:<пароль>@<IP>[:<порт>][/<путь>]
```
ВНИМАНИЕ! Настоятельно не рекомендуется использовать данные доступа администратора.
Для SFTP сервера лучше создать chroot, например, как описано [тут](https://wiki.archlinux.org/title/SFTP_chroot).

## Оповещение о движении

Опционально приложение может уведомлять о срабатывании детектора движения камер.
Оповещение срабатывает в момент появления нового изображения с камеры в указанной папке SFTP сервера.
Для работы этой функции требуется настроить камеры и сервер хранения полученных изображений.
Подробно эти настройки описаны в параллельном проекте [Cams-PWA](https://github.com/vladpen/cams-pwa).

Подробное обсуждение приложения: [habr.com/ru/post/654915](https://habr.com/ru/post/654915/)
и сервера: [habr.com/ru/post/597363](https://habr.com/ru/post/597363/).

[<img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png"
alt="Get it on Github"
height="80">](https://github.com/vladpen/cams/tree/main/app/release)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/com.vladpen.cams/)

&nbsp; [<img src="https://user-images.githubusercontent.com/3853013/194689050-e6da2f21-9aa3-4662-9b7d-7293b140f22f.svg"
alt="Доступно в RuStore"
height="57">](https://apps.rustore.ru/app/com.vladpen.cams)

*Copyright (c) 2022-2024 vladpen under MIT license. Use it with absolutely no warranty.*
