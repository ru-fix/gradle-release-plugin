Плагин для изготовления релизов в соответствии со схемой https://portal.fix.ru/pages/viewpage.action?pageId=29136911

Задачи:  
 * createReleaseBranch - создает новую ветку, предложит ввести название при выполнении задачи  
 * createRelease - Создает релиз. Необходимо запускать на релизной ветке
    
Параметры:
 * mainBranch: String, - название основной ветки, по-умолчанию - "develop"
 * releaseBranchPrefix: String - префикс для релизной ветки, по-умолчанию - "releases/release-"


Подключается как обычный плагин по координатам ru.fix.platform.plugins:release:1.0-SNAPSHOT

### Как подключить к проекту

```
import org.gradle.kotlin.dsl.*
import ru.fix.gradle.release.plugin.release.ReleaseExtension

buildscript{
    repositories {
        maven(url = "http://artifactory.vasp/artifactory/ru-fix-repo/")
    }

    dependencies {
        classpath("ru.fix:gradle-release-plugin:1.0.3")
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
    
### Сборка    
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