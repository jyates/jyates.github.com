---
layout: post
title: HBase Eclipse Support
---

# HBase Eclipse Support
February 3rd, 2012 - San Francisco, CA

(WARNING: I'm going to preach a little bit about IDE's. If you don't care, skip on down to the <a href="#hbase">hbase changes</a>).
Using a 'real' IDE is becoming increasingly popular among developers today - it's come to the point that a large factor in considering language maturity is its IDE support. In particular, its not uncommon to hear people ask, "Is there an Eclipse plugin for that?" 

Now, there are many 'purists' who would argue that using an IDE makes for worse developers. I won't argue that it _can_ make lazy developers, who are tied to their tools, but truly 'worse' is really extreme. The reality of the situation is that these tools are really powerful and open up development to people who are not command line wizards, to whom vim or emacs is an incomprehensible jumble (in fact, universities are only reinforcing this attitude, rather than really teaching kids this days about how to really use the terminal). 

The new argument has now become IntelliJ vs XCode vs Eclipse, not command line vs IDE. This really is the reality now, and we are likely to progress more towards s gui interface, though any real work will probably always be done with text, not shiny blocks and lines (which is why LABView is an absolute kludge, at least for any 'real' coder). The power of the IDE really comes from the the fact that it lets you work on multiple levels at once - you can see the line you are writing, but at the same time, have visual queues as to how it fits into the rest of the project structure as well as the rest of the file. This is incredibly powerful as it lets you *keep more in your head than before*, which anyone who works on code must see as a benefit. And this doesn't even take into account all the power of the refactorings, searching, hot-links, auto-building, etc that these environments provide. 

On the counterpoint, a lot of IDEs can be a _huge_ pain in the a$$, making it seem like your build is working when its not (which is why things like maven from the command line must be considered the final source of truth) or not finding classes when you do things outside the IDE (like running 'mvn clean'). So yeah, they aren't perfect, but they are getting better all the time and at an increasingly rapid pace as they see increasing adoption. This isn't anything new, so get used to it folks. 

So yes, you could do all the fancy stuff that an IDE can do from emacs, but let's face it, most people don't care to put all the time in to learn all the tricks of emacs, write their own modules, etc. They just want it to work. And for a first timer to a project, this is really important to ensure they _want_ to work on it. By lowering the barriers to entry, we make it easier for more people to become involved, which makes the project better. At least in open source.

##<a name="hbase">HBase Change</a> 

For a long time, HBase has technically 'supported' eclipse and even provides [instructions] on how to get it up and running. However, it usually takes a lot of 'jiggering' and then then doing a bunch more 'rejiggering' to make it actually compile. And then if you run any maven commands on the build, well, you are going to probably redo a lot of the 'rejiggering' (and because its a gui, scripting is a pain - point for the command liners).

This all changed with the release of Eclipse Indigo with inclusion of m2eclispe. For those of you who don't know, meclipse is the best plugin for eclipse to integrate with maven. Its automatically pulls in maven depenedencies, supports pom modification and can do full maven builds from within the IDE, and a bunch of other nice utilities; pretty sweet overall. By rolling it into the 'official' java developer release of Eclipse, m2eclipse has gotten much better support and integration with Eclipse. 

In the most recent upgrade, m2eclipse added the idea of a 'connector' to handle 'interesting' lifecycle events that previously would cause the borked project problems in Eclipse. Connectors take care of making sure Eclipse is a awre of these lifecycle events and also make sure there are no classloaders leaked, modification of random files inside workspace or nasty exceptions to fail the build. 

Sounds great, right?

It is, except not all the maven plugins you could want to use have been updated to support it. Would have been nice if the m2eclipse plugin could just run without changing any existing files, huh? Anyways, back in HBase....

In particular, HBase has three plugins that do some pretty important stuff - maven-antrun-plugin, avro-maven-plugin, and jamon-maven-plugin  - and, unfortunately, are not supported by m2eclipse. m2eclipse gets tripped up in the lifecycles of these plugins (even though they bind to pretty standard goals) and justs throws its hands up.

Ususally this wouldn't be a problem, but since m2eclipse is built into Eclipse, it means you can't even get Eclipse to recognize it as a project you can build, so you get this spurious error messages and prevent you from doing certain development within the IDE easily. Lame, right? And because HBase is open source, I wanted to make it _as easy as possible_ for new people to get up and running. Since Indigo has been around for a while, it seemed time to to add full support for what is the 'standard' java IDE.

Eclipse and m2e was actually nice enough to have a 'Quick Fix' for these lifecycle issuee: it adds a few lines to the pluginManagement part of the pom for the

	<groupId>org.eclipse.m2e</groupId>
	<artifactId>lifecycle-mapping</artifactId>

plugin. Essentially, it just lists out the plugins that need to be 'handled' and then tells them what 'action' should be taken for the plugin when its phase is used. 

By default, it just ignores that plugin phase when it builds in eclipse. That doesn't make much sense as the default in most cases though, so frequently you will have to change the action from 'ignore' to 'exectue', which does exactly what you think - allows the plugin to execute when it is 'goal' is executed.

This modifcation is actualy very nicely encapsulated - its doesn't affect any of the other plugin definitions and it doesn't actually need to be run at any point; it just acts as a heads up to m2eclipse. It is unfortunate that the pom does need to be modified, as I don't think it would be that hard to dump this stuff into the eclipse project properties file, but in the end is not a big deal as Indigo is become the defacto standard.  Maybe in the future they will make it a little easier and then we can rip the code back out of HBase.

For the actual code used for the patch, check out [HBASE-5318]. If you are interested in the full details of how to deal with m2eclipse lifecycle stuff, check out the [official documentation].

[instructions]: http://hbase.apache.org/book.html#eclipse
[official documentation]: http://wiki.eclipse.org/M2E_plugin_execution_not_covered
[HBASE-5318]: https://issues.apache.org/jira/browse/HBASE-5318
