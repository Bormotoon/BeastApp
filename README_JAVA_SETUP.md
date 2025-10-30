README: Надёжная настройка JDK / Gradle / Android Studio

Цель
- Сделать стабильную и переносимую конфигурацию Java/Gradle для проекта, чтобы:
  - Gradle использовал Java 17 (требуемую проектом);
  - при отсутствии локального JDK Gradle мог загрузить требуемый toolchain автоматически;
  - участники команды могли легко настроить окружение, а Android Studio корректно синхронизировала проект.

Что уже сделано в репозитории
- `gradle.properties` содержит корректно экранированный путь-фолбэк:
  - `org.gradle.java.home=C\:\\Program Files\\Microsoft\\jdk-17.0.16.8-hotspot`
  - `org.gradle.java.installations.paths=C\:\\Program Files\\Microsoft\\jdk-17.0.16.8-hotspot`
  - `org.gradle.java.installations.auto-download=true` — разрешена автоматическая загрузка JDK toolchains Gradle.
- В `build.gradle.kts` (корень) добавлена безопасная конфигурация Java toolchain для `JavaCompile` задач (запрашивается Java 17).
- В `app/build.gradle.kts` настроены Kotlin toolchain / jvmTarget=17 (`kotlin { jvmToolchain(17) ... }`).

Рекомендация по политике (наиболее надёжный вариант)
- Оставьте `org.gradle.java.home` как fallback (если у вас централизованная CI или фиксированный JDK), но для разработчиков рекомендуем:
  - задать `JAVA_HOME` локально на машине (указывать путь к JDK 17);
  - в Android Studio в настройках проекта выбрать соответствующий Gradle JDK (см. ниже);
  - полагаться на Gradle Java toolchains + авто‑загрузку, если пользователь не хочет/не может установить JDK вручную.

Шаги настройки (для разработчика на Windows)
1. Установите JDK 17 (например, Microsoft OpenJDK, Adoptium или Oracle) и проверьте:

```cmd
"C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot\bin\java.exe" -version
"C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot\bin\javac.exe" -version
```

2. Установите системную переменную `JAVA_HOME` (раз и навсегда):

```powershell
setx JAVA_HOME "C:\Program Files\Microsoft\jdk-17.0.16.8-hotspot" /M
```

(Откройте новый терминал после `setx`, чтобы переменная подхватилась.)

3. В Android Studio: File → Settings → Build, Execution, Deployment → Build Tools → Gradle
   - В поле "Gradle JDK" выберите вашу JDK 17 (обычно Android Studio предложит найденные JDK); можно также выбрать "Use JAVA_HOME" или вручную добавить путь.

4. В Android Studio: File → Sync Project with Gradle Files. Если всё настроено корректно — синхронизация должна пройти.

Команды для локальной проверки (cmd.exe):

```cmd
cd /d C:\VibeCoding\BeastApp
gradlew.bat --version
gradlew.bat assembleDebug --dry-run --console=plain
gradlew.bat assembleDebug --stacktrace
```

Если `assembleDebug --dry-run` или `assembleDebug` выдаёт ошибку toolchain/Java — пришлите вывод консоли (весь текст ошибки). Я помогу разобрать.

Примечания по безопасности и флэкам
- В `gradle.properties` путь сохранён как fallback; если в вашей команде предпочитают не хранить абсолютные пути в репозитории, можно убрать `org.gradle.java.home` и настроить `JAVA_HOME` у каждого разработчика — зато не будет жесткой привязки к локальным путям.
- `org.gradle.java.installations.auto-download=true` позволяет Gradle скачать нужный JDK, если локальный не найден. Это удобно в CI и для новых участников.

Типичные решения проблем
- Ошибка "Could not create task ... Cannot find a Java installation ..." — обычно означает, что Gradle не нашёл JDK с требуемой версией. Проверьте `gradle.properties`, `JAVA_HOME` и Gradle JDK в Android Studio.
- Если Gradle показывает странные ошибки при чтении `gradle.properties` — убедитесь, что пути экранированы (как в проекте: `C\:\\...`).

Дополнительные действия (опционально, для CI)
- В CI лучше явно указывать JDK через переменные окружения или через образ контейнера. Можно также включить `org.gradle.java.installations.auto-download=true` и позволить Gradle скачивать toolchain.

Если хотите, могу:
- Убрать `org.gradle.java.home` из репозитория и добавить инструкции для разработчиков, как настроить `JAVA_HOME` (вариант для крупных команд);
- Помочь настроить CI (YAML) так, чтобы использовался точный JDK 17;
- Если после ваших действий в Android Studio синхронизация всё ещё падает, пришлите текст ошибки "Build" или "Gradle Console" — разберу детально.

---
Файл автоматически создан, содержит конкретные команды для Windows (cmd.exe / powershell) и рекомендации для Android Studio.
