---
layout: post
title: Rolling Java GC Logs
location: San Francisco, CA	
description: In java 1.6_34, rolling GC logs was added. However, the documentation is wrong and hard to find. Here's how you manage it
tags: java, gc, log rolling, bug, enable
---
If you are running a java process, you probably want to keep track of what the garbage collector is doing. You can access this via jconsole or by logging the gc actions by adding:

{% highlight bash %}
-Xloggc:gc.log -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps
{% endhighlight %}

which logs to the 'gc.log' file. 

And for simple cases, that will probably work just fine...until your process starts running row more than a few days. The GC log is not rolled automatically, potentially resulting in a log that can easily grow out of control and fill up your filesystem.

Bad news bears!

What you really want to do roll the logs periodically. You could do this manually with a cron job (which means you might missing some elements), or every time you restart the process (but if you don't restart often, you're up a creek) or send the log to your own custom logger (which is can be tricky to get right). 

All pretty ugly solutions. I sure wish we had something better...

As of Oracle Java 1.6_34 (or 1.7_2 in the latest minor version), we do! GC logs can be automatically rolled at a certain size and retain only a certain number of logs.

To turn on simple log rolling, you only need to add (in addition neccessay gc log arguments mentioned above) to your java command line options:

{% highlight bash %}
-XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=<number of files> -XX:GCLogFileSize=<size>
{% endhighlight %}

where `<number of files>` is just an integer and `<size>` is the size of the file (e.g 16K is 16 kilobytes, 128M is 128 megabytes, etc.). Rolled files are appened with .`<number>`, where earlier numbered files are the older files.

Suppose you ran an java program with the parameters:

{% highlight bash %}
$ java -Xloggc:gc.log -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=128K
{% endhighlight %}

you might see something like the following show up in your directory:

{% highlight bash %}
-rw-r--r--   1 jyates  staff    90K Nov  5 18:25:39 2012 gc.log.0
-rw-r--r--   1 jyates  staff   128K Nov  5 18:25:25 2012 gc.log.1
-rw-r--r--   1 jyates  staff   128K Nov  5 18:25:29 2012 gc.log.2
-rw-r--r--   1 jyates  staff   128K Nov  5 18:25:33 2012 gc.log.3
-rw-r--r--   1 jyates  staff   128K Nov  5 18:25:36 2012 gc.log.4
{% endhighlight %}

What's really nice note here is that GC logs beyond the specified number are _automatically deleted_, ensuring that you know exactly (+/- a few kilobytes for the occasional heavy load) how many log files you will have.

Pretty cool!

Unfortunately, if you attempt to turn on log rolling and forget to include the number of files or the size, the jvm will not turn on logging and instead tell you:

{% highlight bash %}
To enable GC log rotation, use -Xloggc:<filename> -XX:+UseGCLogRotaion -XX:NumberOfGCLogFiles=<num_of_files> -XX:GCLogFileSize=<num_of_size>
where num_of_file > 0 and num_of_size > 0
GC log rotation is turned off
{% endhighlight %}

*this is wrong!*

Double check your other parameters, and try again; you definitely want to use -XX:+UseGCLogFileRotation.

Hopefully this helps you setup your own log rolling. If you have any other JVM/GC tricks, I'd love to hear about them. 

Notes:

1. This is actually a best effort rolling process. If you are doing a lot GC work (e.g. leaning on the 'garbage collect' button in jconsole), the log may grow larger. However, as soon as the jvm has a chance it will then roll the log. 
