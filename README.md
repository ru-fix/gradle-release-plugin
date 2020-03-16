# Gradle Release Plugin
[![Maven Central](https://img.shields.io/maven-central/v/ru.fix/gradle-release-plugin.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22ru.fix%22)

gradle-release-plugin automates release procedure for gradle based projects.
It can automatically create new release branches and tag releases. 
New release is created by updating version in project root `gradle.properties` file.
In this file plugin updates `version` property in format `version=x.y.z`.
Plugin uses auto incremented version number.
Then commits this update in distinct revision tagged by this version number.

- [Short hints](#short-hints)
   * [Minor fix with createRelease](#minor-fix-with-createrelease)
   * [Major change with createReleaseBranch](#major-change-with-createreleasebranch) 
- [Plugin tasks](#plugin-tasks)
    + [createReleaseBranch](#createreleasebranch)
    + [createRelease](#createrelease)
  * [How to use plugin](#how-to-use-plugin)
  * [Gradle Release Flow](#gradle-release-flow)
    + [Principles](#principles)
    + [Release procedure](#release-procedure)
    + [Masterless branching](#masterless-branching)
- [New Project template](#new-project-template)
  * [Travis and Maven Central](#travis-and-maven-central)
  * [Generate .travis.yml](#generate-travisyml)
- [Gradle Release Plugin project details](#gradle-release-plugin-project-details)
  * [How to build this project](#how-to-build-this-project)
    + [Deploy this project to remote repository](#deploy-this-project-to-remote-repository)
 
# Short hints

## Publish minor change in release branch with `createRelease` command for project with multiple release branches

Create and merge minor fix or feature into release branch `release/1.3` with git.
```text
└ master
└ release
  └ 1.3 <-- (current branch)
  └ 1.4
tag 1.3.0
tag 1.3.1
tag 1.4.0
```
Now you are ready to create new release with gradle-release-plugin.  
Since last tag for 1.3 was 1.3.1, then new tag will be 1.3.2.  
```shell script
git checkout release/1.3
gradle createRelease
```
```text
└ release
  └ 1.3 <-- (current branch)
  └ 1.4
tag 1.3.0
tag 1.3.1
tag 1.3.2 (new tag)
tag 1.4.0
```

## Publish minor change in release branch with `createRelease` command for project with single release branch

Create and merge minor fix or feature into release branch `production` with git.
```text
└ production <-- (current branch)
tag 1.3.0
tag 1.3.1
tag 1.4.0
```
Now you are ready to create new release with gradle-release-plugin.  
Since last tag is 1.4.0, then new tag will be 1.4.1.  
```shell script
git checkout production
gradle createRelease
```
```text
└ production <-- (current branch)
``` 

### Publish major change with `createReleaseBranch` command
Create new major version branch with gradle-release-plugin.
```text
└ release
  └ 1.3 <-- (current branch)
```
```
git checkout release/1.3 
gradle createReleaseBranch
git push
```
```text
└ release
  └ 1.3
  └ 1.4 <-- (current branch)
```
Now you have new branch `release/1.4`.  
You can create feature branches based on `release/1.4` and start developing new features.

# Plugin tasks

All plugin tasks work with git repository.  
In order to ensure actual state of local repository they try to fetch data from remote repository.
If remote repository requires authentication, plugin tasks will try to resolve credentials.  
User can provide credentials via:
 * gradle properties:   
    `-Pru.fix.gradle.release.login=<git.login>`     
    `-Pru.fix.gradle.release.password=<git.password>`
 * system properties:  
  `-Dru.fix.gradle.release.login=<git.login>`  
  `-Dru.fix.gradle.release.password=<git.password>`
 * ssh key in user home directory
 * If plugin could not authenticate via properties or ssh key it will prompt user for login and password in interactive mode via console.
  
## createReleaseBranch task

Creates new branch in local git repository.   
By default current branch will be used as a base branch and target branch name will be in format `/release/x.y`.   
During execution, command asks user to specify major and minor version in `x.y` format.
Plugin will look for existing release branches and suggest user a new version by default. 

```
# ----- before -----
└─master  <-- current branch
└─release
  └─1.0
  └─1.1
└─feature
  └─my-new-future

gradle createReleaseBranch
> 1.2

# ----- after -----
└─master
└─release
  └─1.0
  └─1.1
  └─1.2  <-- current branch
└─feature
  └─my-new-future
``` 
Configuration:
  * not required
  
Optional properties:
 * ru.fix.gradle.release.login: String - login for remote git repository
 * ru.fix.gradle.release.password: String - password for remote git repository
 
### createRelease task
 
Searches for existing tags in repository. 
Select tags with names matching version template `x.y.z`.    
Finds latest version among them.    
Calculates new version by incrementing latest tag version by 1.    
Stores new version in `gradle.properties` file in format `version=x.y.z+1`.     
Commit `gradle.properties` file with new tag name `x.y.z+1` into repository.  
User should run createRelease task on one of release branches `release/x.y`.

![](docs/gradle-release-plugin-release-tag.png?raw=true)
    
    
Configuration:
 * releaseBranchPrefix - prefix for release branch name, by default - `/release/`
 * commitMessageTemplate - by default `Release v{VERSION}`
 * tagNameTemplate - by default `{VERSION}`
 * templateVersionMarker - by default `{VERSION}`
 * nextReleaseVersionDeterminationSchema - by default MAJOR_MINOR_FROM_BRANCH_NAME_PATCH_FROM_TAG.   
 Other possible option MAJOR_MINOR_PATCH_FROM_TAG
 
Optional properties:
 * ru.fix.gradle.release.login: String - login for remote git repository
 * ru.fix.gradle.release.password: String - password for remote git repository
 * ru.fix.gradle.release.checkoutTag: Boolean - whether to left repository with checkouted tag or with checkouted release branch. 
 Useful for pipelines. 
 By default createRelease will left repository pointing to release branch.
 * ru.fix.gradle.release.releaseBranch: String - which branch to select for release. E.g. `release/1.2` specify that release should be created based on `release/1.2` branch. By default current branch will be used in order to create release.  

```
# ----- before -----
└─master
└─release
  └─1.0
  └─1.1 <-- current branch
  └─2.0
tag 1.0.0
tag 1.1.0
tag 1.1.1
tag 2.0.0

gradle createRelease

# ----- after -----
└─master
└─release
  └─1.0
  └─1.1  <-- current branch
  └─2.0
tag 1.0.0
tag 1.1.0
tag 1.1.1
tag 1.1.2 (new tag with updated version in gradle.properties file)
tag 2.0.0
``` 
Plugin by default tries to push changes by itself if ssh key or other credentials provided.  
You have to push new tags manually only if plugin fails to resolve credentials.
```shell script
git push --tags
```


## How to use plugin

Add plugin to project gradle build script  
* Kotiln DSL
```
import org.gradle.kotlin.dsl.*
import ru.fix.gradle.release.plugin.release.ReleaseExtension

buildscript{
    dependencies {
        classpath("ru.fix:gradle-release-plugin:$version")
    }
}

apply {
    plugin("ru.fix.gradle.release")
}

//not required for default configuration
configure<ReleaseExtension> {
    releaseBranchPrefix = "release/"
}
```
* Groovy DSL
```
buildscript {
    dependencies {
        classpath "ru.fix:gradle-release-plugin:$version"
    }
}

plugins {
    id "java"
}

apply plugin : "ru.fix.gradle.release"


//not required for default configuration
'ru.fix.gradle.release' {
    releaseBranchPrefix = "release/"
}
```


Manually create branch `/master` with latest version of project.  
```
└─master <-
```
Create new release branch with gradle plugin `gradle createReleaseBranch`  and specify version `1.0`.
```
gradle createReleaseBranch
> 1.0

└─master
└─releases
  └─release
    └─1.0  <--
```  
This will create new branch `/master` -> `/release/1.0`  
Checkout branch `/release/1.1`  
Apply changes in your code. 
Create new tag with updated gradle.properties by running `gradle createRelease`
```
gradle createRelease

└─master
└─release
  └─1.0
tag 1.0.1 
```  
This will create new tag `1.0.1`
This tag will contain single change: updated gradle.properties file with content:
```
version=1.0.1
```

Now CI can checkout tag `1.0.1`, build and publish your project `gradle clean build publish`.

You can merge new fixes to `/release/release-1.0` and create new tagged releases:  
`gradle createRelease` will create new tag `1.0.2`.
```
gradle createRelease

└─master
└─release
  └─1.0 <--
tag 1.0.1
tag 1.0.2  
```  
If you decided to publish new major version based on `/master` branch you can create new release
branch `gradle createReleaseBranch` and specify version `1.1`.  
This will create new branch `/master` -> `/release/1.1`
```
# ----- before ----- 
└─master <--
└─release
  └─1.0

gradle createReleaseBreanch

# ----- after -----
└─master
└─release
  └─1.0
  └─1.1  <--
tag 1.0.1
tag 1.0.2
```  

## Gradle Release Flow

![](docs/gradle-release-plugin.png?raw=true)

### Principles
- Latest stable functionality is located in `/master` branch
- New features is located in `/feature/feature-name` branches
- Maintainable releases is located in `/release/x.y` branches
- Project version is specified in root file `gradle.properties`, field `version=x.y.z`
- Version in `gradle.properties` file in all branches is committed as `x.y-SNAPSHOT`. 
 This will prevent conflicts during merge requests between feature branches `/feature/*`,
 release branches `/release/x.y` and `/master` branch
- During release new tag is being created that holds single commit that modifies `gradle.properties` file 
and specify particular `version=x.y.z` inside the file.

### Release procedure
Suppose that we already have last version of project in `/master` branch, and release `/release/1.2`
- New branch is created based on `/master`. 
Branch name is `/release/1.3`. 
Plugin task `createReleaseBranch` could be used for that purpose.
- New branch `/release/1.3` is stabilized, and changes is added through Merge Requests.
- When branch `/release/1.3` is ready, user launches CI build server release task and specify given branch
 `/release/1.3`
- CI build server task checkout `/release/1.3` branch, then executes gradle command `gradle createRelease`
- gradle plugin searches in local git repository for all tags that matches `1.3.*` template, if there is no such 
tag found 
then default `1.3.1` will be used. Otherwise max tag will be incremented, e.g. if plugin find `1.3.7` then new tag 
name will be `1.3.8`  
- In file `gradle.properties` `version` property is replaced from `1.y-SNAPSHOT` to `1.3.8`
- `gradle.properties` is being committed with new tag name `1.3.8`

### Masterless branching
You can maintain repository without master branch.
And create new release branches from previous ones.
For example, with createRelaseBranch you can create release branch based on currently selected branch. 
```
# ----- before ----- 
└─release
  └─1.0 <--

gradle createReleaseBreanch

# ----- after -----
└─release
  └─1.0
  └─1.1  <--
```  
Masterless flow works in the same way, the only exception is that you do not need to merge all changes to master branch all the time. 
