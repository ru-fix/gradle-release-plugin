# Usages

## Plugin tasks

 * createReleaseBranch - creates new branch in local git repository, by default release-x.y. Asks user to provide 
 major and minor version x.y
 * createRelease - Search for existing tags in repository. Increment version, store in gradle.properties file. Then
 commit gradle.properties file with tag name into repository. User should run createRelease task on one of release 
 branches.
 Parameters: -Pgit.login=<git.login> -Pgit.password=<git.password>
    
Configuration:
 * mainBranch: String - base branch name, by default - "develop"
 * releaseBranchPrefix: String - prefix for release branch, by default - "releases/release-"

### How to use in projects build

```
import org.gradle.kotlin.dsl.*
import ru.fix.gradle.release.plugin.release.ReleaseExtension

buildscript{
    dependencies {
        classpath("ru.fix:gradle-release-plugin:1.2.14")
    }
}

apply {
    plugin("release")
}

//not required for default configuration
configure<ReleaseExtension> {
    mainBranch = "develop"
    releaseBranchPrefix = "releases/release-"
}
```

## Release flow
### Principles
- Main functionality is located in `/develop` branch
- New features is located in `/features/feature-name` branches
- Maintainable releases is located in `/releases/release-x.y` branches
- Project version is specified in file `gradle.properties`, field `version=x.y.z`
- Version x.y.z consist of first part `x.y` which is maintained manually and second part `z` which is generated 
automatically during release process
- Version in `gradle.properties` file in all branches is committed as `1.0-SNAPSHOT`. This will prevent conflicts 
during merge requests between feature branches `/features/*`, release branches `/releases/release-x.y` and `/develop` 
branch
- During release new tags is being created that holds single change set that modify `gradle.properties` file and 
specify particular `version=x.y.z`

### Release procedure
Suppose that we already have last version of project in `/develop` branch, and release `/releases/release-1.2`
- New branch is created based on `/develop`. Branch name is `/releases/release-1.3`. Plugin task `createRelease` 
could be used for that purpose.
- Version inside `gradle.properties` does not changes, it stays `1.0-SNAPSHOT`
- New branch `/releases/release-1.3` is stabilized, new changes is added through Merge Requests.
- When branch `/releases/release-1.3` is ready user launches CI build server release task and specify given branch
 (/releases/release-1.3)
- CI build server release task starts gradle and provide Gradle Release Plugin command: createRelease
- plugin searches in local git repository for all tags that matches `1.3.*` template, if there is no such tag found 
then default `1.3.1` will be used. Otherwise max tag will be incremented, e.g. if plugin find `1.3.7` then new tag 
name will be `1.3.8`  
- In file `gradle.properties` `version` property is replaced from `1.0-SNAPSHOT` to `1.3.8`
- `gradle.properties` is being committed with new tag name `1.3.8`
- Plugin takes first 10 symbols of git revision
- New assemble version will be `x.y.z-rev`, where `x.y.z` - from `gradle.properties` `version` field and rev - 
first 10 symbols of git revision


# Gradle release plugin project    
## How to build
To build and deploy gradle release plugin project to local maven repository run:
```
gradle build publishToMavenLocal
```

### Deploy to remote repository
Provide credentials for repository:  
```
~/.gradle/gradle.properties

repositoryUser = user
repositoryPassword = password
repositoryUrl = url-to-repository
```
Specify version in
gradle.properties
```
version=x.y.z
```
commit new tag with name x.y.z  
then run
```
gradle build publish
```
