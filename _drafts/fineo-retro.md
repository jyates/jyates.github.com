---
layout: post
title: "Hard isn't Valuable: Looking back on Fineo"
location: San Francisco, CA
tags: startup, retrospective, fail, lessons, iot
regenerate: true
---

I've decided its time to wrap up Fineo. I took a shot for a while (nearly two years!), but I'm _way_ past my original time deadline to get traction and well out of (allocated) money. I've spent the last few weeks writing up some of the interesting architecture/design work I did, so at least there is some decent record. At the same time, I've been reading a bunch to understand _where and how_ I went awry.

# Emotional Impact

Quiting Fineo felt a bit like getting broken up with as a teenager. Spending an inordinate amount of time together, Fineo became a large part of my identity (not a good idea). Once its over, it then still takes a while to really accept that it happened - its over; the necessary obsessing over (and over and over) what happened, why it didn't work out, etc. At the end of it, it wasn't necessarily something I regret doing, but it's taken a bit of time to get used to the idea that I'm not going to be working on this thing that was a huge part of my life for nearly two years.

It's been an emotional roller coaster, not to sound too cliché, filled with stress, depression, flow and yes, a couple wins. At the same time, its been a great pressure to expand my comfort zone and learn oodles of things I didn't even consider before. I've said for a while that even if Fineo failed (predestination?), it would still have been worthwhile.

# Business Path and Pitfalls

With any startup, there are inevitably challenges and mistakes made. Without a business person along for the ride, there were probably more than most. However, looking at back at some of the core missteps I made in starting Fineo, there are three over-arching personal challenges that made failure inevitable:

* hubris
* impatience
* loneliness

compounded by a core mistake of conflating [_hard_ with _valuable_](https://betterhumans.coach.me/how-to-beat-your-hard-equals-valuable-bias-81cb9825f4cb), and not spending enough time talking to a range of customers (Read "What customers want", it makes this super obvious).

Oh, and running out of money - that didn't help :)

## Beginning: Streaming SQL + Analytics

Starting a company was something I'd always wanted to try and had saved money for years for just that reason. By summer of 2015 I felt I had a very comfortable amount and was in a position where I had relatively few responsibilities. Startups looked challenging and I've got a track record of finding that hardest thing that captures my interest and doing that - the more potential for pain, the better; harder must be better, right? **_Right?!_**

I looked around at the burgeoning IoT market and thought,

{% quote %}
That's the next big data challenge.
Surely what we were doing at Salesforce is applicable out there.
Managing and leveraging all the data is certainly going to be hard.
{% endquote %}

So, I started working on an idea I had been percolating to build a fast SQL data analytics tool for streaming and scalable data. Certainly seemed challenging and a whole lot more interesting that what I was working on previously.

### Talking to Customers...Errr, right.

I didn't go off completely without validation. I'd talked to a handful of folks and had some initial interest in a solution to "big data for IoT" - the buzziest of words. A couple people mentioned on the 10-100ms range for feedback (particularly from server logs, but that generalizes to devices, right?) and challenges at analyzing data at scale. At the same time, the partners I thought would come with decided to stay at their current positions (can't fault them!), leaving me to strike out on my own. 

"No matter", I thought, "I can do this on my own. I can hustle. I can slog through the crap. I can sell this great idea. And I can code like crazy."

Yeah, right.

### Nope, nope, just... no.

So I spent that first month at home grinding out this super cool data tool. Coding 10, 12 hours a day. Finally, it got to the point I could show it and it looked like any time-series backend product... it wasn't very exciting to look at. So, I spent some time hooking it up to an existing frontend UI and... it looked like just another time series database tool, and didn't articulate **_why it was so much better_**.

But, I was finally starting to go the right direction - showing things to people, figuring out what people are actually struggling with. And it turns out that people almost never needed what I had built (5ms latency SQL-based analytics). I'd had one internal case from Salesforce, and my knowledge that it was interesting and novel _technology_, to base my work on; turns out, **solution looking for a problem**.

However, I'd still only shown it to a handful of people. This was not nearly enough to get a sense of customer needs (or desired outcomes) or have a base of people to whom I could sell the product (which should be those very people to whom I talked that had the problem).

### Streaming Database Platform: Business Analysis

Basically, we were competing with a large amount of _non-consumption_. Major corporations' - generally leaders in the open source data space - need for a scalable, SQL stream tool did not merge until nearly a year later, but it _still wasn't a common issue_. For most, there wasn't enough pain to justify a complete switch, while at the same time many tools emerged to help keep that original system alive.

I also didn't focus on the IoT market - the platform background blinded me to just making a product that was tailor suited to the IoT/device needs. The emerging IoT market is still in the very nascent stages, where the winners are generally those who have an _integrated, high-performing solution_, rather than componentized architecture.

<img src="/images/posts/fineo-retro/componentization.png">

On the left, you generally are going to have the integrated companies that can move fast and deliver lots of end user value. On the right side, you have the componentized companies that are good at delivering incremental value powered by increasingly better components.

With the fully integrated stack, companies can more quickly deliver a product that directly solves the customer problem. As they become more componentized, the integrated stack slows them down as they cannot compete across the standardized 'metrics of value' for the customer, forcing the architecture to become more componentized with stricter interfaces between components. This allows companies to more quickly replace components with higher performing ones. This is also the point where the main product becomes commoditized, and the value gets driven into the component makers (e.g. an analytics/database layer).

Fineo was positioning as a better component in a space where an integrated product enables IoT companies to succeed. At the same time, the component market in which we played was crowded -  each tool/component option provided a modicum differentiation, where Fineo's did not either (a) make it clear why you want it, or (b) wasn't useful enough. Again, focusing too much on the high level differentiation between databases, rather than solving for the focused IoT case (but, this also might not exist).

OK, back to the story so we can see how this gap in understanding continued to plague Fineo.

## Pivot 1: Enterprise NextSQL

I'd been building enterprise big data for years, so certainly that is something worthwhile (or so the thinking went). Back to the code cave and to build with an architecture to execute SQL at scale (leveraging my recently gained knowledge, if not direct work) across online and offline data stores, while providing the flexibility of NoSQL - tada: NextSQL! (or metalytics, as I later came to call it).

Then I proceeded to spin a story around how IoT needed the flexibility of NoSQL, but still wanted to same SQL interface and obviously required the big data scale.

Taking that idea on the road a little bit, I'd talked to tens of companies and found some interest. 

Only one had sustained any interest past the next week.

None bought.

However, I was out talking to people and getting some feedback. And I really don't like talking to people I don't know, so it all _felt_ useful.

I was still missing that core component of an understanding of the problems facing these companies right now. Most of them had a database that worked pretty well for them (generally an RBMS, like Postgres or MySQL) and needed to focus on getting the core device sold. I was frankly scared to go to bigger companies - sales we outside my comfort zone, though I did make some initial attempts - that would be able to actually leverage what we were solving.

And even worse, I knew that was a key weakness - sales, business - and didn't spend a majority of my time looking for someone to help fill that ability. I mean, it seemed like it was going OK and, come on, I'm certainly smart enough to do that work too...right?

### Interlude: Contract Work

During the fall and winter of 2015 my father had health issues, so I was splitting much of my time between the business and trying to help him out (no mean feat, while living across the country). In the spring I took some contract work for a company using [Apache HBase](https://hbase.apache.org) and interested in the SQL layer on top, [Apache Phoenix](https://phoenix.apache.org), projects I'd been working on for the last six years and a core contributor to both projects.

This was a good reset, providing a more rigorous schedule and helped refill the coffers a bit. At the same time, I found my first 'real' customer through that work, interested because he was in the big data industry and understood the niche I was looking to fill. 

Validation! So maybe I _was_ building the right thing!

Now, I just needed to get back to work and finish the damn platform.

## Building the Platform and Growing a Team

About this time I started to realize that I couldn't manage it all on my own. Splitting my time between business and coding wasn't working out. And quite frankly, I knew I was crap at sales and marketing, and needed some help.

So, started to plumb my network for folks that could help. I could put together an 'advisory group', but no one I could convince to come on full time. But always with tantalizing caveat of "sure, when you raise." But, at least now I had a story around help from experienced folk in various fields that I didn't know a damn thing about.

At the same time, the platform was starting to come together. I could read and write to it, we had solid testing infrastructure with comprehensive coverage and prod-like deployments. It was everything that I would want from how a 'real' system should be run.

However, we were still setup for a very "high touch" integration and lacked an easy way for customers to get started. Which means lots of talking to people and manual sales on a technical basis, since there still wasn't a good visualization component.

There was some more interest from a couple of companies and a second company commitmented to being our Beta customers.

### Recruiting

Around the same time, a year into the company, and I had grown quite lonely. An introvert by nature, I was to a point where I was craving more social interaction, but was still hamstrung by my timidity around meeting and talking to new people. I'd taken up drinking socially much more frequently (makes it easier just chat!) and personally noticed that it reached a somewhat concerning point, but shrugged it off in passing jokes.

Imagine my excitement when a contact reached out and was interested in joining me! Finally, someone else who (a) gets it, and (b) has time to work. With a seemingly complementary skill set and a channel to folks that also might be interested and have some time, I was pretty excited.

Finally, the momentum was picking up. I mornings full of meetings with potential new folks and had more things to manage. And busier is better, right!?

However, it was not a match meant to be. I ended up spending most of my time worried about what my potential co-founder was doing, how to best use them. All my suggestions of things to work on were met with positive responses, but things didn't seem to be progressing; I was more and more busy, but less and less felt like it got done.

Queue lots of insomnia and mini-panic attacks worrying about making things work. I'd never had lots of issues with that and had started taking some drugs to help get to sleep regularly. Not a good situation for anyone.

In the end, I had to end the relationship and move on. Partially, so I could sleep and go back to manageable increasing anxiety, and in part so I could re-focus on building the product and getting customers.

### Pivot 1b: Hosted Timeseries Database

We also started position ourselves as a hosted time-series database built for the enterprise (e.g. high availability, reliability, etc.). This is an even more limited market than the generic database market, with even smaller differentiation points. At the same time, there are huge switching costs for customers between databases (moving the historical data while transitioning new data access). This compounds with the fact that many of our prospective customers - small IoT startups - were still on those traditional RDBMS systems that were working well enough. So we had to offer a dramatically lower cost (hard) and better performance (nope, not yet).

At the same time, we also had a smaller range of features from a traditional database - we cut out certain capabilities to enable the wider scale. However, this makes it even harder to transition from the existing infrastructure.

Here, we could have focused on tools to enable transitioning databases or focusing on the wider 'big data' market where we could win on price at a lower SQL feature set (i.e. traditional low-cost disruption).

## Pivot 2: Hosted IoT Platform

At this point, I came up to my original, arbitrary deadline of January 2017 to get funding. But, winter is a bad time to raise, so I pushed that deadline out further. I also knew that my pitch was hurting because (a) didn't have enough validation (i.e. traction) and (b) no co-founder. We were also well past the funding hype of 2015, so turning a profit started looking increasingly important too.

A great piece of advice I got was to turn the challenges around and look at it as a _distribution_ problem - make it a numbers game to make it at least seem like we had more traction.

Well, many of the people I talked to about what I was building were developers and they _got it_, understanding why it was interesting and novel and hard. Sounds like exactly the kinds of people to attract as users. Now, rather than going out and talking to a bunch of IoT developers for what exactly they need, I figured that as a developer myself I could certainly know what would work.

It was back to the code cave, this time build out a sign-up system for users and a UI dashboard so people could actually _see and touch_ what I was building. If you can't touch it, it doesn't really exist.

The initial UI looked good and it was interesting to work with a new technology (even if it was wildly frustrating at times), so my technologist side was satisfied.

At the same time, I also started doing some more 'sales-y' work: scraping together a list of a couple thousand contacts to start cold calling. And I had some initial interest with that (50% open rates!), but it was well outside my wheel house and I found it hard to summon the mental effort to continually pursue it.

### Developer IoT Platform: Business Analysis

Leveraging the big data expertise and our no-operations architecture model, we could compete well with the wider hosted database market (but [that
's  pretty crowded](www.defstartup.org/2017/01/18/why-rethinkdb-failed.html)) or focus on the general IoT Platforms. The integrated platform worked competing against Amazon's tool focus (making an integrated product against their ethos) and had the added advantage of many people using AWS by default (lowering the switching costs).

However, we are still looking at a relatively modular play, in an integrated market (we stopped at the edge of the cloud). We looked to move further up the stack (where there is higher value) with some basic dash-boarding capabilities, but that's not necessarily something we could turn on overnight. The bet was around providing the core interfaces and DB capability that was suited to a market that didn't want to deal with scaling and evolving data (which people rarely (unfortunately) think about deeply, upfront), while maintaining an familiar interface (i.e. SQL).

But there is also a core problem in a data Platform-as-a-Service (PaaS) - few companies are willing to give their data to a startup. They can lose it, are probably likely to go down and might not even be around in 6 months.

{% highlight ruby %}

It feels like everyone is racing to provide the shovels,
while there are relatively few people actually digging for gold

{% endhighlight %}

The only people that can take that risk are startups themselves, but they are rarely going to need to scale benefits of Fineo that are so core to what we offered. So, why trust a startup when I could easily run MongoDB or Postgres for much of my data; at a bigger scale, I could turn to plenty of big companies (Samsung, AT&T, Google) that provide time-series database-as-a-service offerings.

I'm not sure how the proliferation of the 'modular integrated' IoT platforms will fare. These are things like ATT or GE that purport to provide a the core features you need for an IoT application in a quickly composable way (separate from AWS which enables _all the things_). It feels like everyone is racing to provide the shovels, while there are relatively few people actually "digging for gold". It might be that we end up with a market that quickly moves to a componentized model because the value of the components is so high. Or it might be that the various cloud providers enable some entrenchment and can provide the integrated capabilities for a while.

# Places Fineo Could Go

There are a couple of obvious things I could have pursued in the current climate:

1. Partner with an IoT gateway/platform as the time-series database component
  * A more integrated experience for the IoT company that is focused on delivering value for their customer. We do have some of the best tech for this, if I do say so myself.
2. Provide a service for managing device data once it passes through the AWS IoT Gateway. Solve the 'what now?' problem.
  * AWS still has lots of little caveats across a huge range of potential services.

But I'm getting tired and stopped having fun a long time ago.

# Retrospective

What would I have done differently? A whole hell of a lot.

Basically, I did everything completely backwards. **Just wanting to start a company and having a couple of indicators you are on the right path _isn't enough_**. Here's the order I'd try next time:

1. Apply tech to dramatically lower cost of solving problem
2. Find companies/user who have problem
3. Get them to sign a letter of intent for your solution. More is better.
4. Get a cofounder.
	a. Helpful if they are complementary skill-wise, but most importantly someone to completely trust
5. Raise money
6. Quit regular job

## What went right?

I was looking an am emerging, disruptive technology (IoT) which was increasing the availability of data to people that previously didn't have it (i.e. new market disruption). With our no-ops approach we could come in even lower cost the existing 'incumbents' in the market (i.e. low-cost disruption) and provided us with a strong competitive advantage in that we implement changes to the platform almost as fast we could write it. At the same time, we also had a strong technical advantage from my Big Data/Open Source background that enabled us to approach bigger data volumes than most of our competitors.

However, with an almost pathological avoidance of deep/wide customer conversations and a fundamental misunderstanding of the state of the industry, combined with a heaping amount of hubris, it was always an uphill battle.

## In the end

Nearly two years into Fineo, I've run out of network contacts for a potential co-founders and, frankly, am tired and pessimistic about the prospects for _another IoT platform_. The struggle to call it quits has been rough: I've spent much of the last 4 months in a deep depression (rivaled only by one or two other episodes in my, admittedly short, life), but am still convinced that what I was doing was _novel_ and _interesting_.

I've always been fairly successful at anything I've tried (middle-class white, male privilege helps _a lot_), so deciding to quit has been mentally hard to grasp - I've powered through a marathon on broken legs, [sent myself to the hospital to finish an Ironman]; certainly, this isn't the line for me, is it?

Just like a after a breakup, I'm struggling to find something that really excites me. The experience has kindled an increasingly entrepreneurial nature; there isn't a week that goes by when I'm not bugging my fiance with another 'great' idea. But, right now I need to get a real job to recover and provide some stability.

Unfortunately, generally only "lame" companies are using cool technology (i.e. advertising, sales, etc.), and vice versa - all the 'save the world' companies are using conventional software stacks.

But if you are working on something that fits the 'worthwhile' and 'cool tech' stacks, I'd love to hear about it!

### What's next?

For myself, I'm resolving to be more humble, more patient and more outgoing. The business side of things is definitely fun and something I'm going to be pursuing and reading about more more, but I doubt I'll move too far from the keyboard yet.

Starting Fineo has helped me realize - truly, deeply _know_ - how much I don't know and can't do alone. In fact, I suck pretty hard at parts of this startup thing ([though I've gotten better](https://www.youtube.com/watch?v=xzYO0joolR0) at some bits). And I now get that change, regardless of context, takes a certain amount of time, of buy-in, of pure _hustle_ and sometimes you just need to beat your head against it. 

Am I happy with how this all turned out? No, not really.
Would I do a lot of it differently? You bet your ass.
Do I regret having tried? Nope. At least, not most of the time.
Would I do it again? Yeah, I think so.
Soon? Maybe not.

I couldn't have gotten even half way through any of this if it weren't for the incomparable support (emotional and business-wise), understanding and patience of my fiance, [Megan](https://www.linkedin.com/in/meganliamos/) - a brilliant product manager and transcendent baker in her own right. Thank you.


[sent myself to the hospital to finish an Ironman]: /2014/12/06/ironman-cozumel-results.html