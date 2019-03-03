---
layout: post
title: Dockerizing Jenkins Maven builds
location: San Francisco, CA
subtitle:
tags: docker, maven, jenkins, build, bug, root, cannot delete
---

Many legacy build pipelines leverage Jenkins. If you get lucky, you will at least find the time to move to a [Jenkinsfile](https://jenkins.io/doc/book/pipeline/jenkinsfile/) - the same power as Jenkins, but now actually codified, rather than fragile point and click.

As apps start to move to containers, you will probably want to also run your buildsn inside Docker containers. This has the advantage that the build environment can match the develop environment can match the production environment.

# Standard Use

The [Jenkins pipeline documentation](https://jenkins.io/doc/book/pipeline/docker/) implies you can just drop in a pipeline by specifying the 'agent' as a specific docker image.

```
Jenkinsfile (Declarative Pipeline)
pipeline {
    agent {
       docker {
        image 'node:7-alpine'
        args '-v $HOME/.m2:/root/.m2'
      }
    }
    stages {
        stage('Test') {
            steps {
              sh 'mvn -B'
            }
        }
    }
}
```

In theory, this will allow you to mount your maven cache inside the container to help speed up the build (rather than re-downloading the internet each time). There are myriad options for leveraging containers to improve your build, from sidecar containers to using Dockerfile to specify the build environment and more. Its worth reading the docs to get started (if you haven't already)!

# 

Unfortunately, to get reasonable performance and solid isolation in your builds, its a little more complicated than the standard guides readily describe. For many people (particularly those with multi-language monoliths) you will want to have different environments and multiple build steps. This is where you want to leverage standard software practices and try and be DRY. Additionally, this will also makes it easy to update constants when changing your build from a single place.

Let's start with a maven project, where we want to keep the cache around for multiple build runs:

```
DOCKER_MAVEN_IMAGE = 'maven:3.5.2-jdk-8-alpine'
DOCKER_MAVEN_ARGS = '-v $HOME/.m2:/root/.m2'

pipeline {
  stages{
    stage('load') {
      agent {
        docker {
          image DOCKER_MAVEN_IMAGE
          args DOCKER_MAVEN_ARGS
        }
      }
      stage('Test') {
        steps {
            sh 'mvn test' 
        }   
      }
    }   
  }
}
```

Cool, that's pretty simple. But what if we want to also build, run tests, etc. on pull requests?

## Managing Multibranch Pipelines

That's where you need to have a more complex multi-branch pipeline. This generally happens when you want to build a `master` and a `release` branch and run tests off of pull request branches. In this case the standard maven cache mounting args will likely cause an issue if you are running more than one build per server. Remember, the **same cache is mounted into all the build containers!** 

Best case, you merge a PR that shouldn't have been because it got an artifact from the master build. Worst case, you release broken code from a PR to production (so pretty bad).

To avoid this, you will need to mount each branch to a separate directory. This ensures that your master and release branches continue to build quickly and correctly, as well as subsequent PR builds (unfortunately the first build of any branch will need to re-download all the necessary dependencies).

```
DOCKER_MAVEN_IMAGE = 'maven:3.5.2-jdk-8-alpine'
// Bind workspace m2 repo to not download internet too many times. New builds will have to download jars once, but should have minimal thrash for later runs. We don't bind $HOME/.m2 to ensure independence across builds
DOCKER_MAVEN_ARGS = '-v $HOME/.m2/builds/$BRANCH_NAME:/root/.m2'
...
```

Additionally, to ensure that we actually have the ability to write files, we also need to mount container root user as the local root user. If you always know the `jenkins` user id, you can specify this user (and skip the next step), but in complex environments that have evolved over time this is rarely easy or possible. Thus, our argline evolves to add a -u flag for the docker command.

```
DOCKER_MAVEN_ARGS = '-v $HOME/.m2/builds/$BRANCH_NAME:/root/.m2 -u 0:0'
```

## Cleaning up builds

After a build you probably have a cleanup stage. This ensures that the next build executes correctly and without any leftover state. This can be a problem as the image runs as the root user, and because of the way containers are run the files generated will also be root owned, so they will not be able to be deleted by the Jenkins process!

To get around this you should make sure to run a maven clean command after your stage. The end result of all this tuning would look like:

```
DOCKER_MAVEN_IMAGE = 'maven:3.5.2-jdk-8-alpine'
DOCKER_MAVEN_ARGS = '-v $HOME/.m2/builds/$BRANCH_NAME:/root/.m2 -u 0:0'
pipeline {
  stages{
    stage('load') {
      agent {
        docker {
          image DOCKER_MAVEN_IMAGE
          args DOCKER_MAVEN_ARGS
        }
      }
      stage('Test') {
        steps {
            sh 'mvn test'
            // cleanup generate artifacts to ensure build can be cleaned up
            sh 'mvn clean'
        }
      }
    }
  }
}
```

# Bonus Tip: Loading the Shared Library

You can get even more DRY by leveraging a [shared library](https://jenkins.io/doc/book/pipeline/shared-libraries/), where you can abstract a lot of thse commands into something simpler.

Sometimes when running a multi-branch pipeline you can get into cases where the wrong library gets loaded or jenkins can't find a Git SHA (particularly if you are using BitBucket!). In that case, you can maintain your build stability by added a loading phase for the library at the beginning of the build. That _could_ make the build look as simple as:

```
LIBRARY = "my-lib"

pipeline {
  stages{
    stage('load') {
      steps {
        script {  library(LIBRARY).my.path }
      }
    }
    stage('test') {
      steps {
        script {  library(LIBRARY).my.path.Maven().test() }
      }
    }
  }
}
```

A bonus advantage of this approach is that you can easily test new shared library versions by changing the constant to point to your test branch, like `LIBRARY = "my-lib@branch"` and quickly iterate.
