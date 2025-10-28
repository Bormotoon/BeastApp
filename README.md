# BeastApp

Android-приложение для отслеживания прогресса тренировок Body Beast.

## Требования
- Android Studio (рекомендуется последняя версия)
- Android SDK (compileSdk = 35)
- JDK 17 (рекомендуется)

## Быстрый старт (Windows)
1. Откройте папку проекта `BeastApp` в Android Studio (File > Open).
2. При первом открытии студия выполнит синхронизацию Gradle и скачает зависимости.
   - Примечание: gradle-wrapper.jar будет сгенерирован Android Studio автоматически во время синхронизации. После этого сборка из командной строки заработает.
3. Создайте виртуальное устройство (AVD):
   - Tools > Device Manager > Create Device > (например, Pixel 7) > Next
   - Выберите системный образ (Android 14/15), скачайте при необходимости > Finish
4. Запустите приложение:
   - Выберите конфигурацию `app` и устройство в верхней панели
   - Нажмите Run ▶

## Сборка из командной строки (Windows cmd)
После первой синхронизации в Android Studio:

```bat
gradlew.bat assembleDebug
```

APK появится в `app\build\outputs\apk\debug\`.

Юнит-тесты:

```bat
gradlew.bat testDebugUnitTest
```

Инструментальные тесты (нужен запущенный эмулятор или подключённое устройство):

```bat
gradlew.bat connectedDebugAndroidTest
```

## Настройка окружения (Windows, опционально для системного Gradle)
Если у вас установлен Gradle и JDK:
- Gradle: `C:\Program Files\gradle-9.1.0`
- JDK: `C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot`

Пропишите переменные окружения (выполните в cmd от имени администратора, затем откройте новое окно cmd):

```bat
setx -m JAVA_HOME "C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot"
setx -m GRADLE_HOME "C:\Program Files\gradle-9.1.0"
setx -m PATH "%PATH%;%JAVA_HOME%\bin;%GRADLE_HOME%\bin"
```

Проверьте версии:

```bat
java -version
gradle -v
```

Сгенерируйте Gradle Wrapper (создаст отсутствующий gradle-wrapper.jar) и убедитесь, что используется Gradle 9.1:

```bat
cd C:\VibeCoding\BeastApp
"%GRADLE_HOME%\bin\gradle.bat" wrapper --gradle-version 9.1
```

После этого можно собирать через wrapper:

```bat
gradlew.bat assembleDebug
```

## Настройки проекта
- minSdk = 24
- targetSdk = 35
- Язык: Kotlin
- UI: AppCompat + ViewBinding (без Compose, можно добавить позже)

## Структура
- `app/src/main/java` — исходный код
- `app/src/main/res` — ресурсы (layout/values)
- `docs/` — спецификации и документация

## Примечание по CI
В репозитории добавлен GitHub Actions workflow `.github/workflows/android-ci.yml`. Он автоматически установит Gradle на раннере и сформирует wrapper при его отсутствии (на версии 9.1), затем соберёт debug APK и запустит юнит-тесты. Для ускорения локальной работы рекомендуется после первой синхронизации в Android Studio закоммитить сгенерированный `gradle/wrapper/gradle-wrapper.jar`.
