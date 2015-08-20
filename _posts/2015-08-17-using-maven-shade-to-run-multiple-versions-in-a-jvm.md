---
layout: post
title: Using Maven Shade to Run Multiple Versions in a JVM
location: San Francisco, CA
subtitle:
description: Sometimes, you will want to run multiple versions of the same library in the same JVM. Maybe you are writing a framework to run arbitrary user code or maybe you are just integrating with legacy code; either way, you will need a way to run both versions of some library in the same JVM - enter the maven-shade-plugin. It would be lovely if it were just a drop-in solution, but - as with most things maven - there are a few caveats
tags: maven, shade, java, jvm, versions, conflict, resolve, library
---

The default java build tool is still, unfortunately, Maven - despite some great work in things like Gradle and Groovy (lotta 'g' names, weird) - because it can do everything you could possibly need and then some. Unfortunately, as many know, it can be particularly obtuse. For now, lets talk about using the [maven-shade-plugin](https://maven.apache.org/components/plugins/maven-shade-plugin/) to build a custom artifact that allows you to run two different versions of the same library in the same JVM.

Yup, its a little bit of a weird case, but more common than you would expect; I've found it traditionally comes up when running a web server and integrating with established java libraries (e.g. [dropwizard](http://www.dropwizard.io/)/[ratpack](http://ratpack.io/) and Hadoop or [Calcite](http://calcite.incubator.apache.org/), often due to older versions of [Guava](http://guava-libraries.googlecode.com/)).

In this case, I was running a ratpack front-end and leveraging [camel-netty4-http](http://camel.apache.org/netty4-http.html) to receive messages from my stream processors. The split was made a Camel provided a quick and dirty internal facing endpoint with a lot of 'nice' tooling around send/receive pipelines, tracing, etc., while at the same time Ratpack was picked for the client facing work since it has a lot of UI facing niceties (easy separation of static assets, built in websocket and server-sent event support) and is streaming/async native, allowing for minimal overhead for the client interactions.

At some point, Camel will probably be replaced with Ratpack, but to enable running both at the same time (long term viability testing, etc) there is a fundamental mismatch - both libraries leverage different versions of netty!

To resolve this, one of the things the maven-shade-plugin does is allow you to rebundle libraries under a different namespace, which resolves classpath clashes. Then the module that rebundles that jar can be used as a dropin replacement... with some caveats.

# Rebundling Camel Netty4

Lets start with a [simple pom](https://gist.github.com/jyates/fc3d9b427099b750184c) that shades the primary dependency and the transitive dependencies that we care about.

We include all the dependencies in the shaded jar, but only shade the maven parts. This lets us make the shaded jar a drop-in replacement for all the camel libraries and their dependencies.

Now, in the module where you actually care about running both libraries you would do:

{% highlight xml %}
<dependencies>
    <!-- Camel as an abstraction for interacting with the webserver -->
    <dependency>
      <groupId>com.jyates</groupId>
      <artifactId>camel-netty4-http-shaded</artifactId>
    </dependency>
...
</dependencies>
{% endhighlight %}

## Caveats

The plugin only supports including dependencies that are compile or runtime scoped. Unfortunately, this means when you depend on this module (well, the output shaded jar) you will also pull in all the transitive dependencies... which means you end up with the same classpath conflict we tried to avoid originally! Ideally, we would want to have them at 'provided' scope, but alas, the maven-shade-plugin does not include dependencies ouside of compile/runtime (yeah, you could fork the plugin code and make it so, but... that seems like too much effort)

Ok, you can get around it by bundling the exact jars that you want in your runtime application and never running the two components together in the same JVM while testing. However, that is pretty unsatisfying and will likely end up with a lot of runtime debugging.

### Managing transitive dependencies

The natural thing you would now is just exclude the dependent artifacts from the dependency. However, that was a lot of dependencies we need to exclude and its easy to miss one, which leads to hard-to-debug classpath issues. As of maven3 (really, you are still using maven2? sorry, its manual for you), you can do glob exclusions:

{% highlight xml %}
<dependencies>
    <!-- Camel as an abstraction for interacting with the webserver -->
    <dependency>
      <groupId>com.jyates</groupId>
      <artifactId>camel-netty4-http-shaded</artifactId>
      <exclusions>
        <exclusion>
          <groupId>*</groupId>
          <artifactId>*</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
...
</dependencies>
{% endhighlight %}

Fortunately, this actually does everything we want - it excludes all the transitive dependencies and lets us drop-in replace them with the custom, shaded jar we built. 

Only downside? You build gets a some nasty error messages:

{% highlight bash %}
[WARNING] 'dependencies.dependency.exclusions.exclusion.groupId' for com.jyates:camel-netty4-http-shaded:jar with value '*' does not match a valid id pattern. @ line 70, column 20
[WARNING] 'dependencies.dependency.exclusions.exclusion.artifactId' for com.jyates:camel-netty4-http-shaded:jar with value '*' does not match a valid id pattern. @ line 71, column 23
{% endhighlight %}

Oh well, at least everything works.

Generally, you won't need to worry about these kinds of transitive issues if you are building a framework that runs external code and want to support artibitrary user code (I learned a lot from looking at the [Apache Storm pom.xml](https://github.com/apache/storm/blob/master/pom.xml)), but if you want to do some crazy stuff like running two different Netty web servers in the same JVM? Well, now you are covered.

Happy shading!

