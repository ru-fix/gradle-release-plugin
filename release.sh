# Since gradle-release-plugin can not use itself to release itselt we have to use this script
git fetch --tags
git tag --list | sort -V
echo Enter new tag version
read V
git checkout -b tmp$V
echo version=$V > gradle.properties
git add gradle.properties
git commit -m "Release $V"
git tag $V
git checkout release/1.3
git branch -D tmp$V
git push --tags