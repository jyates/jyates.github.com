---
layout: post
title: Guest Blogging
location: San Francisco, CA
description: I was recently asked to write a few guest blog posts about HBase. I'd had some ideas bouncing around for a while (and a little personal brand expansion is never a bad thing), so I started working on it. Here's some of my thoughts from the experience
tags: hbase, guest, blog, post, big data, thoughts
---

I was recently asked to write a few guest blog posts about HBase. I'd had some ideas bouncing around for a while (and a little personal brand expansion is never a bad thing), so I started working on it. Here's some of my thoughts from the experience.

The posts that are I wrote were all drawn from my recent experiences working with and on HBase:

 * [HBase Replication - Promise and Peril](http://blog.safaribooksonline.com/2012/11/13/hbase-replication-promise-and-perils/)

    * A discussion on how cross-datacenter replication works in HBase and some of the more interesting things you can do beyond the obvious disaster recovery; its not all sunshine and roses though, as a I talk about in this post.

 * [HBase .META. Layout](http://blog.safaribooksonline.com/2012/11/16/tip-2-hbase-meta-layout/)

    * How does HBase organize .META.? A definitive description (as of HBase v0.96) and some info about how splitting works, why we get occasional 'holes' in .META. and some hints on how to fix it.

 * [Modularizing HBase: Lessons in Maven’s Black Magic](http://blog.safaribooksonline.com/2012/11/20/tip-modularizing-hbase-lessons-in-mavens-black-magic/)

    * A while back, I spent a bunch of time modularizing HBase (HBASE-4336) - taking it from a single monolithic layer to a set of smaller building blocks. Along the way, I learned some useful lessons in dealing with Maven that anyone thinking about modularizing a big project will probably encounter.

 * [HBase File Retention for Backup and Testing](http://blog.safaribooksonline.com/2012/11/14/tip-hbase-file-retention-for-backup-and-testing/)

    * Recently HBase (0.94 and higher) gained the ability to start retaining HFiles when they are deleted. Combined with the existing archival of HLogs we have the building block of a comprehnsive backup and testing solution for you HBase cluster. 

When I was asked to write the posts, I had about a 1.5 weeks - a pretty daunting timeline for four 1000+ word posts that I'd only half formed ideas about. I had another idea that didn't get written about integrating HBase with legacy applications, but thought four posts more than enough work my deadline (maybe something I post on here?). In the end, I'm glad I put a limit.

Generally, my blogging process is fairly organic - I jot down a couple notes over a few weeks about things I might want to write about, then sit down at a cafe for an afternoon and hammer out the post, and then do a couple review passes before posting (by the way, I use git locally for all my documents + [jekyll](https://github.com/mojombo/jekyll) hosted on github - its a great, free way to host a blog and iterate on posts). This is the method I'm using for this post.

With my deadline, my usual meandering pace wouldn't cut it. Luckily, I'd been a bit lax about writing, so I have a couple general ideas already written down. *Two weeks ahead already*

Once I narrowed down the topics I wanted to work on, I spent two afternoons writing outlines. Turns out the skills I learned in college aren't all that rusty. This was great for two reasons: (1) organizing my thoughts into a coherent story and (2) made the writing only require the specific prose, not the ideas as well. 

In software terms, the outline became a bit of an abstraction layer - suddenly the amount things I needed to keep in my head was halved; the ideas were already there, I just needed to make them sound good. Outlines inherently also use a markdown style syntax, so translation to the final document was even easier as I write in markdown (definitely worth digging into the positives of a leaky abstraction - both in terms of mental model and efficiency -  but that's for another post).

Once I had the deadlines, it was just a matter of another two afternoons in the coffee shop to write up the posts. Probably the worst part of that whole process was making the images (aren't my words clear enough??) and converting the formatting to the publisher's desired Word format (as you can expect, not a big fan).

Follow up with a single editing session over all the posts and I was good to go! I would never have written with the same volume in the timeframe without the deadline. Instead, it would have been spread over a few months, with 'recovery time' between posts. There is a certain dark enjoyment though out of burning hot - getting things done is always appealing, even at the cost of a few hours of sleep.

Its hard to say if this was any faster than my usual process. The rigor lead to a more… boring experience, but to overall higher quality posts. I have a tendency to ramble (noticed?) using the organic method (unless its writing about solving a technical problem - see [fixing java GC logging](/2012/11/05/rolling-java-gc-logs.html)) and be a bit more long winded than necessary. This blog is certainly not my masterpiece and while I like producing a higher quality work product, my time is certainly limited. 

>>"Je n'ai fait celle-ci plus longue parceque je n'ai pas eu le loisir de la faire plus courte. (I have made this letter longer than usual, because I lack the time to make it short.)"
>>
>> - Blaise Pascal, Lettres Provinciales (1656-1657), no. 16.

But that's how things go in this era of more, faster, now (if not yesterday); c'est la vie.

What kind of writing techniques do you use? Do you find it more fun to write in a more structured or unstructed environment.
	
