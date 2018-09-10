---
layout: post
title: Partial Multi-module Maven builds for pull requests
location: San Francisco, CA
subtitle:
tags: multi-module, maven, build, partial build
---

As your Maven projets get larger it can take a non-trivial amount of time to complete a build, particularly if you are running each module in sequence due to code limitations. In this case, you should think about only having to build code that changes and its downstream dependencies.

The short answer to this is to use some of the [multi-module flags](https://blog.jayway.com/2013/06/09/working-efficiently-with-maven-modules/).

Let's assume that earlier in our build process we found one module from which there was a change made; let's call the module 'foo'.

Then, we want to build all the dependent modules up to the one we need. This ensures that we generate the correct binaries for the module (that all its dependencies are up-to-date when building later). Theoretically, you could skip this step rely on the already deployment binaries, but that depends on your branching module.

```
# make the module and (-am) all its dependencies
$ mvn -pl foo -am clean install -DskipTests
```

After that we want to build + test the module **and** all its dependent modules.

```
# run the tests for our module and all the modules that depend on it.
$ mvn -pl foo -amd clean verify
```

Maven offers [many different flags](http://maven.apache.org/guides/mini/guide-multiple-modules.html) to work with multi-module projects that are worth looking into!

This is can be a great processes for when you are running builds for each Pull Request and want to make the feedback very quick for developers. Without special tools you can easy have developers waiting 10s of minutes to hours for a build to run all the tests, even if they are only changing one small module in a corner of the code.

While you may be good at identifying changes to modules, maven is not necessarily good at managing dependencies. You often can get surprising transitive dependencies and essentially building through luck (not a sustainable pattern!). So, a word of caution - you definitely want to still run all the tests _at least_ before going to production, but probably also before going to staging. This will help cut down on failures before it is too late.

The ideal thing would be to move to a build tool like [Bazel](https://bazel.build/), which can actually identify the changes made and correctly build the things you need - rigth out of the box! Wnfortunately we don't always have the time (or opportunity) to do the better thing and are stuck with the tools we have.

Now, how do we make find the changes, say in Jenkins, and maybe support it in a multi-language environment? Well, that's a post for another time...
