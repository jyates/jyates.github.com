---
layout: post
title: Heads Down, Thumbs Up
tags: salesforce.com, hbase
---

# {{ page.title }}

May 1, 2012 - San Francisco, CA

If you have been following this blog at all, you have probably left by now. But for those of you still around (and those new!) here's a little explaination as to the recent gap in posts.

At the end of January 2012 I started working at Salesforce.com on the team dedicated to bringing HBase to one of the world's foremost CRM software providers. Despite being a large company, they are still incredibly agile. Further, I get to spend almost everyday working on a project (HBase) that I used to work on for fun. On top of that my team is top-notch. And we are running real code, that impacts real people and makes a difference everyday. 

Some people say they have great jobs, but I have the best job (and snacks!). Here is the view from my window: 

<img src="/images/posts/office.jpeg" alt="San Francisco Ferry Building and Treasure Island - Salesforce HQ: 1 Market Street"/>

This post was not meant to be an explicit plug for Salesforce.com, but we are <a href="mailto:jyates AT salesforce DOT com?subject=#dreamjob">hiring</a>.

Also, in the meantime, I've picked up my marathon training (following this great book: [Run Less, Run Faster] (http://www.amazon.com/Runners-World-Less-Faster-Revolutionary/dp/159486649X)) and am doing 30-40 mi of running a week with cross-training via cycling (fixie or raod bike, depending on the day) and yoga at [Planet Granite] (http://www.planetgranite.com); I'm also starting to get back into climbing now that my finger has healed from a trip to Bishop, CA earlier this month and a two year long fight with tendonitis. This isn't an excuse, merely the reasons behind being more busy of late.

Oh, and I also just moved into a new apartment in Lower Haight with a few great guys. Tons of space, peek-a-boo view of downtown, good landlord and quiet neighbors; pretty much what everyone is looking for in an apartment (and crazy low rent). This means I've been spending an exhorbinant amount of time doing things for my aparment; its mostly little things or massive Ikea runs, but time sinks nonetheless. 

And in the rest of my free time I've been working as much as I on HBase. Its been a couple months, but the code is just starting to come together on a lot of these Jira issues. For those interested, here are the main things I've been working on:
* [HBase-50] (https://issues.apache.org/jira/browse/HBASE-50) - Snapshots (also the longest standing ticket in HBase)
* [HBase-5547] (https://issues.apache.org/jira/browse/HBASE-5547) - Get a reference to a table in the shell
* [HBase-4336] (https://issues.apache.org/jira/browse/HBASE-4336) - Convert HBase into maven modules
* [HBase-5548] (https://issues.apache.org/jira/browse/HBASE-5548) - Don't delete HFiles in backup mode

So its been a bit busy. Sorry. I'm going to try and be better about it. There is a list of things I've been wanting to talk about, but just haven't gotten around to it yet.

Here is what I have planned (I'll attempt to remember to add links here as they are written):
* using table references in HBase shell
* tips and tricks for using maven in multi-module projects
* philosophical discussion on why money is not an indicator of a person's value, but seems to be all we have
* use and the design of hfile backups - code for this is nearly done, and the only gating factor in my writing
* using HBase snapshotting (and a second post on the architecture) - have to wait until the code is done for this

Definitely some stuff to look forward to, you know, if your into that kind of thing.
