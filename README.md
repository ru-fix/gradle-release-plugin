# Плагин для изготовления релизов

## Управление версиями
### Положения
- Разработка ведется в ветке /develop
- Релизы находятся в ветках /releases/release-x.y
- Версия приложения указана в файле gradle.properties, поле version=x.y.z
- Версия x.y.z состоит из части x.y которая назначается руками и части z которую автоматически инкрементит система 
сборки
- Версия в gradle.properties во всех ветках всегда 1.0-SNAPSHOT чтобы не было конфликтов при создании Merge Requests

### Процедура релиза
- На основе /develop cоздается ветка с названием /releases/release-x.y руками
- Версия gradle.properties не меняется, в ней указан 1.0-SNAPSHOT
- При необходимости ветка стабилизируется, в нее вносятся изменения через Merge Request-ы.
- Когда ветка готова к созданию очередного  релиза в системе сборки запускается задача release с указанием branch 
(/releases/release-x.y)
- Система сборки запускает задачу Gradle Release Plugin: release
- В git ищутся все тэги вида x.y.*,  если тэги не найдены выбирается версия x.y.1, если найдет тег - вибрается версия 
на 1 большая чем самый старший тег, напрмиер если был найден тэг x.y.7 версия назначится x.y.8
- В файле gradle.properties версия приложения заменяется с 1.0-SNAPSHOT на выбранную версию x.y.z
gradle.properties коммитится и создается новый тег с названием x.y.z
- Получаем первые 10 символов git revision
- Собираем сборку, присваивае ей версию x.y.z-rev, где x.y.z - из gradle.properties  version, rev - первые 10 символов
 git revision

##  Команды плагина

Задачи:  
 * createReleaseBranch - создает новую ветку, предложит ввести название при выполнении задачи  
 * createRelease - Создает релиз и пушит созданый тег в репозиторй. Необходимо запускать на релизной ветке. 
 Параметрами  -Pgit.login=<git.login> и -Pgit.password=<git.password> нужно указать данные для доступа в Git
    
Параметры:
 * mainBranch: String, - название основной ветки, по-умолчанию - "develop"
 * releaseBranchPrefix: String - префикс для релизной ветки, по-умолчанию - "releases/release-"


Подключается как обычный плагин по координатам ru.fix:release:x.y.z (см последнюю версию в репозитории)

### Как подключить к проекту

```
import org.gradle.kotlin.dsl.*
import ru.fix.gradle.release.plugin.release.ReleaseExtension

buildscript{
    repositories {
        maven(url = "http://artifactory.vasp/artifactory/ru-fix-repo/")
    }

    dependencies {
        classpath("ru.fix:gradle-release-plugin:1.2.14")
    }
}

apply {
    plugin("release")
}

configure<ReleaseExtension> {
    mainBranch = "develop"
    releaseBranchPrefix = "releases/release-"
}

```
    
### Как собрать проект плагина    
Собрать и опубликовать в лоакльном m2 repository
```
gradle publishToMavenLocal
```

### Deploy to remote repository
Provide credentials for repository:  
```
~/.gradle/gradle.properties

repositoryUser = user
repositoryPassword = password
```
Specify version in  
gradle-release/gradle.properties
```
version=x.y.z
```
run
```
gradle publish

```
return version back in 
gradle-release/gradle.properties
```
version=1.0-SNAPSHOT
```