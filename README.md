Плагин для изготовления релизов в соответствии со схемой https://portal.fix.ru/pages/viewpage.action?pageId=29136911

Задачи:
    * createReleaseBranch - создает новую ветку, предложит ввести название при выполнении задачи
    * createRelease - Создает релиз. Необходимо запускать на релизной ветке
    
Параметры:
    mainBranch: String, - название основной ветки, по-умолчанию - "develop"
    releaseBranchPrefix: String - префикс для релизной ветки, по-умолчанию - "releases/release-"


Подключается как обычный плагин по координатам ru.fix.platform.plugins:release:1.0-SNAPSHOT


    
### Сборка    
Собрать и опубликовать в лоакльном m2 repository
```
gradle publishToMavenLocal
```