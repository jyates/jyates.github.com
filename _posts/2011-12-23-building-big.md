---
layout: post
title: Building Big
---

# Building Big
## Or, how to make shit work (at scale)

December 25, 2011 - Gaithersburg, MD

A lot of this post is based on my recent discussions with a few companies in the bay area who are 'changing the paradigm'. Ok, what they are really doing is moving to a cloud infrastructure. But in a world domainated by SQL and big iron, it has to change how people run their backend.

Along with this change comes the issue how to build these massive systems. And then how to make them go fast. And _then_, once you have all of that, you need to make sure multiple products within your company can actually use this stuff. We'll talk about each of these in turn.

## Make sure you have a problem

I can't stress this enough. It's an epidemic among engineers that we build these massive systems that no one needs or cares about. And they end up sucking. 

Any time you are building something new, you need to really make sure that you need what you are building. Here is where Eric Reis's [The Lean Startup] makes a ton of sense - talk to people who are going to use you backend, figure out what their _problems_ are - _not_ what they want to do with your technology. It's about building in that feedback loop from the beginning. This also works really well with traditional agile methodolgies - get feedback from the 'client' and iterate based on their responses. 

## Building from scratch

One of my favorite types of interview questions is the "design a system to do X", where X is a general goal, which has to run at massive scale; bonus points if it is something you are actually building now or have had to build. I like this question for two reasons. First, because it gives you real insight into a how a person thinks about doing the exact things you are _already_ building. What do they worry about? What do they rely on the technology for? And most importantly, what kind of process do they use to solve a real problem? 

The other reason I like that kind of question is that its pretty close to how you are going to architect the solutions to your own problems (or at least it should be). 

Step 1: I have to solve this problem, meet this SLA, support this application. 

Step 2: ?. 

Step 3: profit. 

Turns out the '?' is the fun part. Whiteboard iteration works really well here - they cost you only a couple of hours, but help you refine to a solution to meet your general design goals. Once you have that, you can actually start building and see what breaks.

First, start with the dumb solution - it doesn't scale, single points of failure, basically the works. Then start removing/replacing things following the principles below and you will end up with a scalable system that actually solves the problem. But the point is that it will work (theoretically). From there, you can refine for certain properties - scale, fault tolerance, security, etc. The beauty here is that you can literally scrap the entire design so far if it doesn't conform to what you need (and you shoudn't be afraid to erase everything) since it is the swipe of an eraser, rather than huge swaths of code. 

You might stumble across the right solution by taking the dumb one and iterating on it as a physical product, hammering it until it breaks, fixing the broken stuff and then repeat.  However, without designing the scale from the start, you get locked into a product that is inherently built to _not_ scale. 

Its always much harder to destroy things that you spent blood and sweat on rather than some drawings on a whiteboard. However, once you are actually using a system it can turn out that you didn't account for all the use cases or things didn't work quite as you expected; then, you can't be afraid to lay waste to huge swaths of code or even rewrite everything (assuming you have time). 

###Design it right the first time (or scrap it and try again)

Let's take a look at an example that has seem some controversy recently: MongoDB. I'll readily admit that there are a lot of benefits of the system - its wicked easy to use, it integrates easily, schemaless as expressive as necessary, and it handles a lot of annoying things for you. 

However, what Mongo isn't is a _cloud-scale database_. From the start, it was designed as a single-server NoSQL database (don't get me started on why NoSQL != cloud) when then adding sharding and 'scalability' on later. This lead to data loss in some cases and a set of really painful 'features' - lack of good monitoring, manual restarts and re-partitioning on failure, collections don't shard, etc. These added properties were not designed into the system from the start - they break the original paradigm and really need a system redesign to be done 'properly'.

Another case is adding security into Hadoop - a freaking mess. On the flip side is security and scale in Accumulo; it had two basic goals and it does those two things pretty well (not saying anything about the quality of the code...).

When you start diverging from the original intentions of a system, be it a database, a product, etc, you need to seriously consider if the original design of that system is sufficient; chances are that after pivoting they aren't quite right. Thankfully, there are the traditional danger signs for these things - rewriting massive pieces, constant bugs in certain areas, 'dirty' hacks. 

Once you start accumulating enough indicators (this is a feel thing), it's  time to consider doing a massive rewrite. What you are pivoting the system on is longer solid, but instead has become a unstable, uncertain bog that doesn't quite do what you need and also doesn't do the original purpose. 

##General principles:
When doing the original design, or a rewrite, there are certain properties you need to make sure a built into the system from that start. Otherwise, its going to be huge pain later or just be duck-taped together. 

### 1. No single points of failure

This has been the bane of the Hadoop stack for years now and mountains of work have gone into making a high-availabilty namenode. Multiple companies have roled their own solutions because they realized that if shit goes down and their namenode crashes, so does their business. So that can't happen. And look at MapR - they are killing it right now because they did a great implementation and make it plug-and-play (more or less) for enterprise. This is really closer to the way things need to be; its what Oracle has become and part of why Apple is great: _it just works_. 

Cloud is not that way now, though we can see some work 'enterpise proofing' the Hadoop stack. Once people can just throw in a cloud cluster, jam data in there, and it works and scales will be the beginning of the end for Oracle.
_*Death to Oracle*_. Not that I have anything against those guys (I admire what they have done), but we are at the point where real-deal companies know Oracle doesn't scale and these companies are starting to kill because they can leverage all their data. But I digress...

###2. Parallize everything

This is higly complementary to not having a single point of failure - on this side of the coin its really about making sure your producers and consumers match up. What this parallelization doesn't mean is just throwing a bunch of machines at the problem - if you don't design your system to handle that, you are going to just waste money and resources. 

This generally means randomizing your key space (hashes work well for this),matching up producer and consumer nodes, and leveraging 'server-side' resources (part of why [HBase]'s coprocessors so freaking awesome). But these are really the basic things - the final answer on how to do this well is, "It depends." It depends on your system requirements, it depends on the kind of data you are storing, it depends on your access patterns. In the end, all of these dependencies will lead to you a set of requirements. Its pretty likely that someone else has come up with these same requirements before (we are not all unique snowflakes) and written something to handle them (give or take), oh and its probably open source. So don't go an build it in-house if you don't have to, but if you do, know why you are building it. 

###3. Make scaling easy (and separate services)

This is a little more subtle as it relies on the fact that you already have 1 and 2. Consider the way Amazon is setup [^one]: everything is based on APIs between different internal products - even if it isn't exposed as a user service, you have to act like it is. This is great because you can do a lot things: innovate independently of other pieces, ease new developers into the company as they only need to learn their one portion (rather than one monolith system) and if you want to make any portion run fast, just put more machines behind that facade.

On the downside, cascading failures can be incredibly hard to debug. Further, there is no tight integration between pieces, which can easily lead to fragmented technology stack, which could run _way_ faster, if only people could work more closely along the stack (the very thing Job's was trying to avoidat Apple). One of the worst parts of this setup (and is true of any system based on APIs) is the proliferation of tools within a company can start to become overwhelming - people need to have seminars and brown bags on the tools available in their own company. 

Wait, what? Sounds like its time to simplify - software is built on abstractions, so what happened to get away from that? You really only need a couple types of backend: traditional, in-memory, cloud scalable; within that you can split on what 2 parts of the [CAP Theorem] you are covering. That's a max of 6 different types of database. However, above that you really should be more tightly integrating products - middle-ware up to frontend. 

Yes, there will be some overlap, but that why you have your architects meet regularly and talk about what they are doing. And why you have people rotate teams, sharing best practices. I like the Apple (I think) standard that its fine to have 3x replication of the same idea/internal product, built by different teams. After that, its used so widely that you get together and build it right, using the best practices from each group to provide a service that will be streamlined _and_ immediately useful to the teams using them.   

What we end up with then is a trend upward in services that are tightly coupled, but general enough that people can build new things on them. Combined with a culture of collaboration, this leads to new tools built with the understanding that it may, one day, be scrapped and rebuilt the 'right way'. 

### 4. Do the right thing
A recent methodology I like is following a server-based approach using apis. Essentially you can get a bunch of servers that will respond to a given api, and you don't worry about how they do it on the backend - you could even make them part of the bigger cluster so you can share basic admin costs (nudge, nudge, wink, wink)!  This means each product claims ownership (to a degress) over all the tools to build their product. This gives you a vertical integration from the bare metal up to the product/service that leads to great products. If you can fine tool how your database works, then you can make your app exceedingly fast. If you can make the data storage intuitve within the app context, then the platform people can iterate fast. Everything just starts to click.

This type of setup starts to get really bad-ass when you can start doing automated deployments over a shared cluster. Using things like [Chef] and [Mesos] combined with some automated cluster load monitoring. All of a sudden you can roll out pieces of your backend as you need it, have it humming along and configured correctly right away, and if you design correctly, will ensure linear(ish) scalabilty.

You still need the guys who make the services run, but they can (and should) work with multiple teams. That has the added advantage of making the tools more generalized (so new products can just be bolted on top) and also makes the systems more accessible and bomb proof. 

You can think of this approach as the horizontal-vertical integration. Horizontally across the organization for each service, vertically within each product or service.

On the low level side of things, this means you have to be able to add machines on the fly to handle load. If you do this correctly, failure recovery comes pretty much for free. 

For example, you have a bunch of pollers reading from a queue. Well, make the key space each poller need to cover dynamically assigned and combined with ack's to make sure messages aren't lost. All of a sudden, suppose a ton more messages come in - add more pollers. On the other hand, suppose a poller goes down; pollers will be notified of a key-space modification and can immediately repurpose and pick up this keyspace change. Kinda like how the [Dynamo]-like systems handle their key-space. If you keep the pub-sub ratio close to one-to-one (or one-to-more) you can get some really blazing fast systems. Personally, I like to use [ZooKeeper] to handle these notifications and monitoring - it scales as much as you need, is pretty freaking reliable (hundreds of days of uptime for a given machine in the cluster are not unusual), and open source.

The one thing you don't want to do is the monolithic deployment. This seems to be popular from the ol' LAMP stack days. If I can deploy my infrastructure as a stack, shouldn't the rest of my application be that way too? NO! If you have more than one product this is pretty infeasible. Further, even with one application, different tiers of that application are going to have different requirements (why do you need as many web servers as you do message handlers?). So avoid that - go horizontal-vertical.

###5. Expect people to be dumb

Within a given product, the people running it don't really care how you are storing the data, just that you do and meet criteria X, Y, Z. Yeah, they may be excited to work with NoSQL, but most people can't handle the complexity. Because you know what? This shit is hard. And running at scale is not the same as running on one big-ass server. 

Right now we need to have some really smart technologist managing our clusters, making sure we have great key design, that data gets aged off appropriately, to write a specialized Map/Reduce jobs to clean up out tables. 

This is crazy. 

Don't get me wrong, this people are doing great work, but we should be working to make them irrelvant (this is the whole point of most technology). And don't worry, they will still have jobs, but just on the next great thing. 

This stuff should be dead simple. I'm calling for easier abstractions on top of the really scalable stuff. For instance, look at [Redis] all can do is dump in a couple of different object types. Simple. And now consider that working across 50 nodes, 100 nodes, 500 nodes. Holy crap - that becomes an incredibly powerful tool because it _allows people to think about things naturally_. Key-Value stores are not really a natural way to thing about things, so people stuggle with low level things like key design, rather than what their stuff actually does (Disclaimer: I love optimizing key design and think it is really important to get high performance applications. But I also think that most people shouldn't have to worry about it).

So what we need is a couple layers of abstraction - this idea of only having a few types of databases, of periodically killing off a group of tool to rebuild it the right way, and tight communication between teams to avoid massive overlap.

## Running at scale

If you aren't don't run whatever you are building at scale, you are building a toy. Unless you are running at scale, no one gives a damn about what you build, what great design patterns you are using, what language its written in, or how hard you worked on the system. 

This means getting your stuff into production as fast a possible (again, following Reis's advice) because it will show you where your shit breaks. This is a good thing, because those points make it 

You will see what features people are using -  hopefully, you only have one or two and got those from talking to the platform people who need your system. Once you have something people will use, you are already on the way to doing good horizontal-vertical integration of the different tool sets.  


[The Lean Startup]: http://
[Redis]: http://
[Dynamo]: http://
[ZooKeeper]: http://
[Chef]: http://
[Mesos]: http://
[HBase]: http://hbase.apache.org
[CAP Theorem]: http://
[^one]: googler rant

