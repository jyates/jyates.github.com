---
layout: post
title: Dev Tip- Using Gradle without hating it
location: San Francisco, CA
subtitle:
description: Gradle is starting to become mature enough to be used as a viable build system. However, when trying to build with gradle there can be some easy idioms to help you up the learning curve
tags: gradle, build, hadoop, jar
---

Gradle is starting to become mature enough to be used as a 'real deal' build system. However, when trying to build with gradle there can be some easy idioms to help you up the learning curve.

# The build script is groovy code

Honestly, this is one of my favorite features of gradle - it makes it incredibly powerful. That said, it can be a bit of a pain if you aren't willing to spend some considerable amount of time learning groovy; instead, you will probably just end up doing an immense of googling to finally cobble together something that works.

An snippet that I found particularly useful, particularly when working with hadoop, but extensible to other depenencies is maintaing a list of basic dependencies, and then iterating them as needed.

{% highlight groovy %} 
def getHadoopDependency(componentName) {
    "org.apache.hadoop:${componentName}:$hadoopVersion"
}
{% endhighlight %}

This will build you the full dependency name of the hadoop component with the right name.

You can store the dependencies you will need across projects in a List

```
List standardHadoopComponentNames = ['hadoop-client', 'hadoop-common', 'hadoop-hdfs']
```

and then use that later to include the dependencies fairly easily:

{% highlight groovy %}
project('myproject') {
    dependencies {
...
       for (component in standardHadoopComponentNames) {
            compile(getHadoopDependency(component)) {
                exclude group: "org.slf4j", module: "slf4j-log4j12"
            }
        }
{% endhighlight %}

This includes each of the 'standard' hadoop components from above as compile-time dependencies and cludes the slf4j-log4j12 dependency from each of the components(1).

I still haven't found a clean way to not have to include all these lines in all the projects (i.e. an inheritance, like you would expect with Maven). However, this is a very simple idiom that becomes very expressive and powerful. It works great for a single project, but can get frustrating with multiple.

# Testing independently

If you are starting to build up a lot of integration tests that are long and likely conflict on the same JVM, you probably want to add a new project and fork each test:

{% highlight groovy %}
project('myproject:it') {
    test {
        // fork a new jvm for each test class
        forkEvery = 1
    }
{% endhighlight %}

# Different Scala versions
This was a tip I picked up from browsing the [Kafka](http://kafka.apache.org) and [Samza](http://samza.apache.org) gradle builds.

Suppose you want to include Scala in your project, but want to support running against a couple of different versions. In you top-level build.gradle file you would just add:

{% highlight groovy %}
apply from: file('gradle/dependency-versions.gradle')
apply from: file("gradle/dependency-versions-scala-" + scalaVersion + ".gradle")
apply from: file('gradle/wrapper.gradle')
{% endhighlight %}

This lets the developer configure, either through the config files for default or on the command-line for dyanmic setting, which version of scala to use. Then in depenency-versions.gradle you would have all the versions of dependencies (like the <properties> section in a maven pom)

{% highlight groovy %}
// All the versions of the dependencies, in one place
ext {
    gradleVersion = "2.3"
...
// You can also include lists of dependencies here too!
 jacksonModules = ["jackson-annotations", "jackson-databind"]
...
}
{% endhighlight %}

Note though, we don't include the scala version in this set of properties - instead it goes into the top-level gradle.properties. Here's one of mine:

{% highlight text %}
group=com.salesforce.my-project
version=1.0.1-SNAPSHOT
scalaVersion=2.10
org.gradle.jvmargs="-XX:MaxPermSize=512m"
systemProp.file.encoding=utf-8
{% endhighlight %}

By default, our build will then look at the dependency-version-scala-2.10.gradle file:

{% highlight groovy %}
ext {
    scalaTestModuleVersion = "scalatest_2.10"
    scalaTestVersion = "1.9.2"
    scalaLibVersion = "2.10.4"
    // Extra options for the compiler:
    // -feature: Give detailed warnings about language feature use (rather than just 'there were 4 warnings')
    // -language:implicitConversions: Allow the use of implicit conversions without warning or library import
    // -language:reflectiveCalls: Allow the automatic use of reflection to access fields without warning or library import
    scalaOptions = "-feature -language:implicitConversions -language:reflectiveCalls"
}
{% endhighlight %}
where we can set all the scala properties we need when adding scala to our standard build cycle:

{% highlight groovy %}
// all projects assumed to have scala, but you can add this just to a specific scala project too.
allprojects{
    // For all scala compilation, add extra compiler options, taken from version-specific
    // dependency-versions-scala file applied above.
    tasks.withType(ScalaCompile) {
        scalaCompileOptions.additionalParameters = [scalaOptions]
    }
}

plugins.withType(ScalaPlugin) {
    //source jar should also contain scala source:
    srcJar.from sourceSets.main.scala

    task scaladocJar(type: Jar) {
        classifier = 'scaladoc'
        from '../LICENSE'
        from scaladoc
    }

    //documentation task should also trigger building scala doc jar
    docsJar.dependsOn scaladocJar

    artifacts {
        archives scaladocJar
    }
}
{% endhighlight %}

# Leveraging projects built with maven

Some projects are built with maven and assume that the environment from which the tests are run is also maven (I'm looking at you Hadoop and HBase projects). For the most part, this is fine... until its not. Frequently, you will end up with cases where your tests create an extra {project}/target directory and store temporary data there. To fix this, you can add a cleanup for that directory to every project pretty easily.

{% highlight groovy %}
allprojects{
   ...
    task deleteMavenBuildDirs(type: Delete) {
        delete "target/"
    }

    // Add removal of the maven build directory since the HBase/Hadoop tools all assume maven build
    cleanTest.dependsOn deleteMavenBuildDirs
}
{% endhighlight %}

This way, everytime the 'cleanTest' target is run, you will also delete all the target/ directories.

# Building Jars & Tars

There are probably a bunch of jars you will want to build for your project. For a single-project gradle build, this is pretty straight forward from the docs. However, once you are into multi-project gradle builds, this can start to get a bit more complicated, especially when looking to release.

For the below, I'm just using a single build.gradle file - I find its easier to reason about the different projects when you can see them all together. However, gradle also lets you have a build.gradle per project directory, allowing you to decouple things when they starting getting too complicated.

## Tests

By default, the 'java' plugin will just build a java jar. However, you frequently will want to reuse your test sources across projects. To do this, you need to build a "tests" type jar (the maven equivalent is the com.mycompany:project:1.0:test artifact) which can be depended on by other projects:

{% highlight groovy %}
subprojects {
    jar {
        baseName = "$project.parent.name-$baseName"
    }

    // build a testjar so we can use the tests resources other places
    task testJar(type: Jar, dependsOn: testClasses) {
        baseName = "test-${project.archivesBaseName}"
        from sourceSets.test.output
    }

    configurations {
        tests
    }

    artifacts {
        tests testJar
    }
}
{% endhighlight %}

This will build not only the standard jar with an intelligent name - by default, it would just be the name of the project, but you may have multiple sub-projects with the the same name, and hence no way to differentiate them - but also the tests har with the standard maven naming conventions.

## Building a distribution tarball

Ok, now you have some artifact that you want to package up all the hardwork you have done and make a release of the build jars (e.g. something that would run on another box).

For this case, consider two projects: myproject:fs and myproject:rest. We want to package up these two projects into a single gzipped tarball.

{% highlight groovy %}
apply plugin: 'base'

// Building the distributions
// --------------------------

configurations {
    distLibs
}

dependencies {
    distLibs project(':myproject:fs'),
            project(':myproject:rest'),
}

task distTar(type: Tar) {
    description = "Build a runnable tarball of all the subprojects"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    compression = Compression.GZIP

    // set the base directory for all the files to be copied
    into("$baseName-$version")

    // generic directory/file includes
    into("conf") {
        from 'conf'
    }

    into("bin") {
        from 'bin'
    }

    // other helpful/necessary top-level files
    from("LICENSE.txt", "README.md")

    // depend on all the sub-projects
    into("project-lib") {
        from { subprojects.jar }
    }

    // brings in all the runtimes dependencies of the sub-projects
    dependsOn configurations.archives.artifacts
    into("lib") {
        from configurations.distLibs
    }
{% endhighlight %}

In that tarball we are going to have a handful of directories:

{% highlight text %}
/myproject-1.1.0-SNAPSHOT   // basename + version as we defined in the gradle.properties
../bin
..../start.sh              // a start script that is in the top-level bin directory of the project, just copied in here
../conf
..../conf.xml              // basic config files, also from the project's top-level /conf directory, copied into the tar
../lib                     // all the dependencies for each of the projects, put into the same directory.
..../some-lib-0.1.jar
..../another-lib-1.1.jar
   ...
../project-lib            // the jars from the projects that we wrote
..../myproject-fs-1.1.0-SNAPSHOT.jar
..../myproject-rest-1.1.0-SNAPSHOT.jar
{% endhighlight %}

All we need to do to build that tarball is then just run ```$ gradle distTar```

And the final tarball will be in the 'distributions' directory.

# Summary

Hopefully, this has been somewhat useful. We've covered how you can leverage some the features of having the groovy language in your build scripts, how to add new tasks, managing your scala versions and how to roll a distribution and its dependent jars.

### NOTES:
(1) Using hadoop with other projects that do logging 'better' generally means having to exclude this dependency as other projects will use slf4j-over-XXXX as the adapter, rather than the direct slf4j-log4j pipe, causing a runtime conflict. There are newer log systems (logback, log4j2, etc) that are faster and more efficient - slf4j-log4j12 is just good enough to ge by, but you can - and should! - do a lot better.
