#!/usr/bin/python3
# Script generates travis.yml configuration for github project for Travis
# - encrypts gradle properties from ~/.gradle.properties
# - encrypts ~/gnupg/secring.gpg key store
# - generates `.travis.yml` template
from argparse import ArgumentParser
import subprocess
import os
import re
import jprops
from jinja2 import Template

parser = ArgumentParser()
parser.add_argument("-p", "--project", type=str, help="Project local git repository location", required=True)

args = parser.parse_args()
projectLocation = args.project

print(f"Project location: {projectLocation}")
print("Use `travis login` if you are running travis client first time")


def run(popenargs) -> str:
    stdout = subprocess.run(popenargs, stdout=subprocess.PIPE, cwd=projectLocation).stdout.decode('utf-8')
    print(f"RUN>>{stdout}")
    return stdout


print(f"Travis version: {run(['travis', 'version'])}")

gradlePropertiesFile = f"{os.getenv('HOME')}/.gradle/gradle.properties"
print(f"Looking for properties in: {gradlePropertiesFile}")

properties = []
with open(gradlePropertiesFile) as file:
    properties = jprops.load_properties(file)

secureItems = ["repositoryUrl", "repositoryUser", "repositoryPassword", "signingKeyId", "signingPassword"]

print(f"Found:")
for item in secureItems:
    print(f"{item}={properties[item]}")

print("encrypt properties")

secure = []
for item in secureItems:
    secure.append("" + run(['travis', 'encrypt', f"{item}={properties[item]}"]).strip())

print("encrypt secring.gpg")

secringFile = f"{os.getenv('HOME')}/.gnupg/secring.gpg"
print(f"Looking for secring.gpg in: {secringFile}")

secringFile = f"{os.getenv('HOME')}/.gnupg/secring.gpg"
if not os.path.isfile(secringFile):
    print("secring.gpg not found")
    exit(1)

secringFileEnc = f"{projectLocation}/secring.gpg.enc"
if os.path.isfile(secringFileEnc):
    print(f"{secringFileEnc} already exist. Removing it")
    os.remove(secringFileEnc)

fileEncryptionOutput = run(['travis', 'encrypt-file', secringFile])
key = re.search('\$encrypted_([^_]+)_key', fileEncryptionOutput).group(1)

travisTemplateString = """
language: java
jdk:
- oraclejdk8
cache:
  directories:
  - "$HOME/.gradle"
jobs:
  include:
    - stage: build
      if: tag IS blank
      install: skip
      before_script: if [[ $encrypted_{{key}}_key ]]; then openssl aes-256-cbc -K $encrypted_{{key}}_key -iv $encrypted_{{key}}_iv -in secring.gpg.enc -out secring.gpg -d; fi
      script: ./gradlew clean build

    - stage: deploy
      if: tag =~ ^\d+\.\d+\.\d+$
      install: skip
      before_script: openssl aes-256-cbc -K $encrypted_{{key}}_key -iv $encrypted_{{key}}_iv -in secring.gpg.enc -out secring.gpg -d
      script: ./gradlew clean build publish
env:
  global:
  - signingSecretKeyRingFile="`pwd`/secring.gpg"
  {% for item in secure %}
  - secure: {{item}}
  {% endfor %}
"""

template = Template(travisTemplateString)
print("* * * .travis.yml * * *")
print(template.render(key=key, secure=secure))
