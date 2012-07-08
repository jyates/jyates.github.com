---
layout: post
title: Building Big
description: A lot of this post is based on my recent discussions with a few companies - both big and small - who are attempting to 'change the paradigm' either of society in general,
tags: scalable, big, system, code, architecture, Amazon, Google
---

# {{ page.title }}
## Or, how to make shit work at scale

January 2, 2012 - Palo Alto, CA

A lot of this post is based on my recent discussions with a few companies - both big and small - who are attempting to 'change the paradigm' either of society in general, their industry, or just their company. 

Turns out what they are really doing is moving to a cloud infrastructure which all of a sudden is enabling a huge amount of innovation (this stuff is pretty damn exciting and is _really_ changing how we interact with the world). But in a world domainated by SQL and big iron, it is also changing how people think about building these systems. 

And it turns out once you start throwing in tons of moving parts, it gets _hard_.

And then you have to make them go fast. 

And _then_, once you have all of that, you need to make sure multiple products within your company can actually use this stuff. 

I'll get to all of them in turn. But let's start with just figuring _how_ all this stuff goes together.

## Make sure you have a problem

I can't stress this enough. It's an epidemic among engineers that we build these massive systems that no one needs or cares about. And they end up sucking. Not having a user segment means that your stuff will never be useful to anyone. That may be fine for personal side projects. However, for systems that a company is spending money on, the 'customer' - in this case the internal devs using the platform - need to be involved from the start.

Any time you are building something new, you need to really make sure that you need what you are building. Here is where Eric Reis's [The Lean Startup] makes a ton of sense - talk to people who are going to use you backend, figure out what their _problems_ are - _not_ what they want to do with your technology. It's about building in that feedback loop from the beginning. This also works really well with traditional agile methodolgies - get feedback from the 'client' and iterate based on their responses. 

This is all about eliminating waste - only build things that are really necessary, everything is just stoking your ego.

## Building from scratch

If you start by designing a massive system and then figure out how to mold that design to your problem, it will only generate a bloated architecture that half-solves your problem. You want to keep in mind the features the system needs as a way to avoid designing yourself into a corner, but the goal needs to be *figure out what you need _first_, then how to build it*. We are at the point now where basically anything you can think up can be built - the hard part is figuring out what you want to build (and if you should).

First, start with the dumb solution - it doesn't scale, single points of failure, but it takes care of the functionality you need, it works. Then start removing/replacing things following the principles below and you will end up with a scalable system that actually solves the problem. From there, you can refine for certain properties - scale, fault tolerance, security, etc. The beauty here is that you can literally scrap the entire design so far if it doesn't conform to what you need (and you shoudn't be afraid to erase everything) since it is the swipe of an eraser, rather than huge swaths of code. 

###Design it right the first time (or scrap it and try again)

The design here is iterating up from that first, naive solution - you have something that works, but doesn't really scale. If you are a startup, then what works here might actually be a phyiscal system, but if you are expecting to scale or have a bit more time, it behoves you to start iterating on that design immediately. Otherwise, you are liable to go through a pain period very soon when things start falling over and you have to rip out everything or limp along on a legacy system that can't really do what you need.

What you really need to do is design up from the ground, before a line of code is written, for it do the right thing. This is really easy now as we can iterate the naive solution for the properties we need. In working through the design, you might run into situations where the dumb solution's assumptions break at scale and you need to redesign. However, you just go back to the start and redo each property from a revised starting point. This new start may be a little more complex that the original, but it will scale better later. The far worse case is trying to take a system beyond its original constraints - it only leads to lots of duct tape, dirty hacks and heartache. Oh yeah, and you will probably have to rip it out in the end.

Let's take a look at an example that has seem some controversy recently: MongoDB. I'll readily admit that there are a lot of benefits of the system - its wicked easy to use, it integrates easily, schemaless as expressive as necessary, and it handles a lot of annoying things for you. 

However, what Mongo isn't is a _cloud-scale database_. From the start, it was designed as a single-server NoSQL database (don't get me started on why NoSQL != cloud) when then adding sharding and 'scalability' on later. This lead to data loss in some cases and a set of really painful 'features' - lack of good monitoring, manual restarts and re-partitioning on failure, collections don't shard, etc. These added properties were not designed into the system from the start - they break the original paradigm and really need a system redesign to be done 'properly'.

Another case is adding security into Hadoop - a freaking mess. On the flip side is security and scale in Accumulo; it had two basic goals and it does those two things pretty well (not saying anything about the quality of the code...).

Once you start accumulating enough indicators that it has become a legacy system, it's  time to consider doing a massive rewrite. What you are pivoting the system on is longer solid, but instead has become a unstable bog that doesn't quite do what you need and now also sucks at it original design goals. So go back, and design it again. You learned some lessons from building it for real, so now you can do it right. 

That's also the value in hiring people who have built big before - they know the problems, they can see the cravasses and can build it right the first time. Unfortunately, these people - the ones who really "get it" - are few and far between in terms of today's cloud technology.

##General principles:
When doing the original design, or a rewrite, there are certain properties you need to make sure a built into the system from that start. Otherwise, its going to be huge pain later or just be duck-taped together. These are the things you need to consider when iterating up from the naive solution. 

### 1. No single points of failure

This has been the bane of the Hadoop stack for years now and mountains of work have gone into making a high-availabilty namenode. Multiple companies have rolled their own solutions because they realized that if shit goes down and their namenode crashes, so does their business. That can't happen. 

Now consider MapR - they are popular now because they did a great implementation and make it plug-and-play (more or less) for enterprise. This is really closer to the way things need to be; its what Oracle has become and part of why Apple is great: _it just works_. Oh, and it scales.

This means need multiple pieces running the process, either dynamically parallelizng the work and/or with hot failover. Either way, you need to have a system that can immediately pick up the slack if a component goes down. It all depends on your SLA as to whether a true hot failover is necessary or if you can just cut down polling intervals and implement a dynamic system.

You also need to make sure if something goes down you get the replacements (either promotion, resharding of work or fail-over) within hard bounds. Eventual consistency is nice here in that you can play fast and loose with these constraints by designing your data to fit the model. However, the things you need to really worry about are availability of the system (recovery happens _fast_) and the holy grail: data is _never_ lost. This means replica's, flushes to disk, and write-ahead-logs. 

###2. Parallize everything

This is higly complementary to not having a single point of failure - on this side of the coin its really about making sure your producers and consumers match up. What this parallelization doesn't mean is just throwing a bunch of machines at the problem - if you don't design your system to handle that, you are going to just waste money and resources.  

This generally means randomizing your key space (hashes work well for this), matching up producer and consumer machines, and leveraging 'server-side' resources (part of why [HBase]'s coprocessors so freaking awesome). 
Essentially you want to get to the point where you have one writer talking to one consumer (+/- a margin of acceptable parallelization on the consumer). Think of one ingest client talking to one data server, with a margin of parallelization of regions per server. 
If you design your key space correctly, then it is pretty reasonable to expect this kind of behavior. This also means you will probably (nearly definitely) need to do some indexing to avoid doing full table scans (this time a one client to many server situation - which is also going to kill you). 
The key here is that we can leverage the fact that storage is cheap, so replicate data as much as you need to avoid locking. Here things like [Culvert] are really nice as they work in-system, scale, and are very flexible to accomodate variable indexing and data schemas.

But these are really the basic things - the final answer on how to do this well is, "It depends." 
It depends on your system requirements. 
It depends on the kind of data you are storing.
It depends on your access patterns. 

In the end, all of these dependencies will lead to you a set of requirements. Its pretty likely that someone else has come up with these same requirements before (we are not all unique snowflakes) and written something to handle them (give or take). Oh and its probably open source. It is very easy to think you need to build a tool, but build in-house if you don't have to; if you do, know why you are building it. 

###3. Make scaling easy (and separate services)

This is a little more subtle as it relies on the fact that you already have 1 and 2. Consider the way Amazon is setup [^one]: everything is based on APIs between different internal products - even if it isn't exposed as a user service, you have to act like it is. This is great because you can do a lot things: innovate independently of other pieces, ease new developers into the company as they only need to learn their one portion (rather than one monolith system) and if you want to make any portion run fast, just put more machines behind that facade.

On the downside, cascading failures can be incredibly hard to debug. Further, there is no tight integration between pieces, which can easily lead to fragmented technology stack, which could run _way_ faster, if only people could work more closely along the stack (the very thing Job's was trying to avoidat Apple). One of the worst parts of this setup (and is true of any system based on APIs) is the proliferation of tools within a company can start to become overwhelming - people need to have seminars and brown bags on the tools available in their own company. 

Wait, what? Sounds like its time to simplify - software is built on abstractions, so what happened to get away from that? You really only need a couple types of backend: traditional, in-memory, cloud scalable; within that you can split on what 2 parts of the [CAP Theorem] you are covering. That's something like six different types of database you _might_ need to maintain, but in reality it will be a closer to two or three. However, above that you really should be more tightly integrating products - middle-ware up to frontend. 

Yes, there will be some overlap, but then your architects meet regularly and talk about what they are doing (right?). And why you have people rotate teams, sharing best practices. 

I personally like the idea that you have three independent teams build a tool with the same basic requriments. They will probably come up with different architectures, each with their own benefits. However, once you have three different use cases and examples, then you have a chance to really build the system with proper abstraction _and_ using the knowledge gained from trying it three different ways. I think this is exactly what Apple does internally. 

What we end up with then is a trend upward in services that are tightly coupled, but general enough that people can build new things on them. Combined with a culture of collaboration, this leads to new tools built with the understanding that it may, one day, be scrapped and rebuilt the 'right way'. 

### 4. Do the right thing
A recent methodology I like is following a server-based approach using APIs as a guideline. Essentially, you can end up with a set of servers that will respond to a given set of APIs, and you don't worry about how they do it on the backend - you could even make them part of the bigger cluster so you can share basic admin costs (nudge, nudge, wink, wink)!  
This means each product claims ownership (to a degree) over all the tools to build their product. This gives you a vertical integration from the bare metal up to the product/service - that leads to great products. 

This type of setup starts to get really bad-ass when you can start doing automated deployments over a shared cluster. Using things like [Chef] and [Mesos] combined with some automated cluster load monitoring. All of a sudden you can roll out pieces of your backend as you need it, have it humming along and configured correctly right away, and if you design correctly, will ensure linear(ish) scalabilty.

You still need the guys who make the services run, but they can (and should) work with multiple teams. That has the added advantage of making the tools more generalized (so new products can just be bolted on top) and also makes the systems more accessible and bomb proof. 

#### Horizonal-Vertical integration
You can think of this approach as the horizontal-vertical integration. Horizontally across the organization for each service, vertically within each product or service.

On the low level side of things, this means you have to be able to add machines on the fly to handle load. If you do this correctly, failure recovery comes pretty much for free. 

For example, you have a bunch of pollers reading from a queue. 
Well, make the key space each poller need to cover dynamically assigned and combined with ack's to make sure messages aren't lost. All of a sudden, suppose a ton more messages come in - add more pollers. 
On the other hand, suppose a poller goes down; pollers will be notified of a key-space modification and can immediately repurpose and pick up this keyspace change. Kinda like how the [Dynamo]-like systems handle their key-space. 
If you keep the pub-sub ratio close to one-to-one (or one-to-more) you can get some really blazing fast systems. 
I find using [ZooKeeper] to handle these notifications and monitoring works really well- it scales as much as you need, is pretty freaking reliable (hundreds of days of uptime for a given machine in the cluster are not unusual), handles all the heartbeating for you and is open source.

## Running at scale

If you aren't don't run whatever you are building at scale, you are building a toy. Unless you are running at scale, no one gives a damn about what you build, what great design patterns you are using, what language its written in, or how hard you worked on the system. 

This means getting your stuff into production as fast a possible (again, following Reis's advice) because it will show you where your shit breaks. This is a good thing, because fixing those pain points will make it useful and fast. 

Most platform people (people in the company using your backend) don't really care how you are storing the data, just that you don't lose their data and meet certain criteria. Yeah, they may be excited to work with NoSQL, but most people can't handle the complexity. Because you know what? This shit is hard - running at scale is not the same as running on one big-ass server. 

Right now we need to have some really smart developers managing our clusters, making sure we have great key design, that data gets aged off appropriately, to write a specialized Map/Reduce jobs to clean up out tables. 

This is crazy. 

This stuff should be dead simple. We need simpler abstractions on top of the really scalable stuff. Developers shouldn't be running the servers, but focusing on building those on those abstractions. This is a big part of why traditiaonl SQL-based databases became so popular - they could be run just by DBA, rather than database software engineers (and at least one order of magnitude cheaper). 

Key-Value stores are not a natural way to think about things, so people stuggle with low level things like key design, rather than what their stuff actually does (Disclaimer: I enjoy optimizing key design and think it is really important to get high performance applications. But I also think that most people shouldn't have to worry about it).

What we need is a couple layers of abstraction - this idea of only having a few types of databases, of periodically killing off a group of tool to rebuild it the right way, and tight communication between teams to avoid massive overlap.

If we can build simple tools for services, that are built into the technology stacks for each product, you end up with very clean designs which make it easy to modify and understand the system. Simple, intuitive interfaces will then tend to generate more innovation. If you build the bottom of these stacks will basic scale in mind, then scaling the each new product becomes a matter of flipping a switch rather than doing a massive redesign. 


[The Lean Startup]: http://www.amazon.com/Lean-Startup-Entrepreneurs-Continuous-Innovation/dp/0307887898
[Redis]: http://redis.io/
[Dynamo]: http://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf
[ZooKeeper]: http://zookeeper.apache.org/
[Chef]: http://www.opscode.com/chef/
[Mesos]: http://www.mesosproject.org/
[HBase]: http://hbase.apache.org
[CAP Theorem]: http://en.wikipedia.org/wiki/CAP_theorem
[Culvert]: http://github.com/booz-allen-hamilton/culvert

[^one]: Stevey's Google Platforms rant - https://gist.github.com/1281299
