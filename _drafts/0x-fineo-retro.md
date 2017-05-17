---
layout: post
title: Looking back on Fineo
tags: startup, retrospective, fail
---

TODO:
  - look at how business model was flawed. 
    - product improvement vs. disruption
    - competition vs. growing new customers

With any startup, there are inevitably challenges and mistakes made. Looking at back at some of the core misteps I made in starting Fineo, there are three over-arching personal challenges that made quitting inevitable:

* hubris
* impatience
* loneliness

compounded by a core mistake of conflating [_hard_ with _valuable_](https://betterhumans.coach.me/how-to-beat-your-hard-equals-valuable-bias-81cb9825f4cb). Oh, and running out of money - that didn't help :)

# In the beginning

Starting a company was something I'd always wanted to try and had saved money for years for just that reason. By summer of 2015 I felt I had a very comfortable amount and was in a position where I had relatively few responsibilities.

Startups looked challenging and I've got a track record of finding that hardest thing that captures my interest and doing that, the more potential for pain, the better. Because, harder must be better, right?

At the same time, grew bored and frustrated by an inability to drive change at Salesforce. I'd seen a few opportunities for improvement, but felt unable to change the inertia to get things done.

Feeling stymied, I looked around at the burgeoning IoT market and thought,

	That's the next big data challenge. Surely what we were doing at Salesforce is applicable out there. Managing and leveraging all the data is certainly going to be hard.

So I started with an idea I had been percolating to build a fast SQL data analytics tool for streaming and scalable data. Certainly seemed challenging and a whole lot more interesting that what I was working on previously.

Now, I didn't go off completely without validation. I'd talked to a handful of folks and had some initial interest in a solution to "big data for Iot". At the same time, the partners I thought would come with decided to stay at their current positions, leaving me to strike out on my own.

"No matter", I thought, "I can do this on my own. I can hustle. I can slog through the crap. I can sell this great idea. And I can code like crazy."

Yeah, right.

# Nope, nope, just... no.

So I spent that first month at home grinding out this super cool data tool. Coding 10, 12 hours a day. Finally, got to the point I could show it and like any backend product... it wasn't very exciting to look at. So, I spent some time hooking it up to an existing frontend UI and... it looked like just another time series database tool.

But, I was finally starting to go the right direction - showing things to people, figuring out what people are actually struggling with. And it turns out that people almost never needed what I had built (5ms latency SQL-based analytics). I'd had one internal case at Salesforce,and my knowledge that it was interesting and novel _technology_, to base my work on; turns out, solution looking for a problem.

Well, shit.

# Pivot 1: Enterprise NextSQL

I'd been building enterprise big data for years, certainly that is something worthwhile. I so went back into the code cave and came up with an architecture to execute SQL at scale (leveraging my recently gained knowledge, if not direct work) across online and offline data stores, while providing the flexibility of NoSQL - tada: NextSQL!

Then I proceeded to spin a story around how IoT needed the flexibility of NoSQL, but still wanted to same SQL interface and obviously required the big data scale.

Taking that idea on the road a little bit, I hit up IoT World to pitch my idea to the companies on display there. I'd gotten some interest and had talked to tens of companies.

I think only one had sustained any interest past the next week. But I was out talking to people and getting some feedback. And I really don't like talking to people I don't know, so it all felt useful.

The core bit I studiously avoided, even though I knew it was a weakness, was the sales process. I had gotten some interest, surely that was good enough. Instead of following a rigorous process, it was slap-dash at best.

And even worse, I knew that was a key weakness - sales, business - and still didn't actively look for someone to help fill that ability. I mean, it seemed like it was going OK and, come on, I'm certainly smart enough to do that work too...right?

## Interlude: Contract Work

During the fall and winter of 2015 my father had health issues, so I was splitting much of my time between the business and trying to help him out (not simple living across the country). In the spring I took some contract work for a company using [Apache HBase](https://hbase.apache.org) and interested in the SQL layer on top, [Apache Phoenix](https://phoenix.apache.org), projects I'd been working on for the last six years and a core contributor to both projets.

This was a good reset, providing a more rigorous schedule and helped refill the coffers a bit. At the same time, I found my first 'real' customer through that work, interested because he was in the big data industry and understood the niche I was looking to fill.

Now, I just needed to get back to work and finish the damn platform.

# Building the Platform and Growing a Team

About this time I started to realize that I couldn't manage it all on my own. Splitting my time between business and coding wasn't working out. And quite frankly, I knew I was crap at sales and marketing, and needed some help.

So, started to plumb my network for folks that could help. I could put together an 'advisory group', but no one I could convince to come on full time. But always with tantalizing caveat of "sure, when you raise." But, at least now I had a story around help from experienced folk in various fields that I didn't know a damn thing about.

At the same time, the platform was starting to come together. I could read and write to it, we had solid testing infrastructure with comprehensive coverage and prod-like deployments. It was everything that I would want from how a 'real' system should be run.

However, we were still setup for a very "high touch" integration and lacked an easy way for customers to get started. Which means lots of talking to people and manual sales on a technical basis, since there still wasn't a good visualization component.

There was some more interest from a couple of companies, but nothing solid materializing beyond a second company's commitment to try the platform.

## Recruiting

Around the same time, I had grown quite lonely. It was now approaching 1.5 years of working alone, and generally at home. An introvert by nature, I was to a point where I was craving more social interaction, but still hamstrung by my timidity around meeting and talking to new people. I'd taken up drinking socially much more frequently and personally noticed that it reached a somewhat concerning point, but shrugged it off in passing jokes.

Imagine my excitement when I contact reached out to me and was interested in joining me working on Fineo! Finally, someone else who (a) gets it, and (b) has time to work. With a seemingly complementary skill set and a line on folks that also might be interested and have some time, I was pretty excited.

Finally, the momentum was picking up. I mornings full of meetings with potential new folks and had more things to manage.

However, it was not a match meant to be. I ended up spending most of my time worried about what my potential co-founder was doing, how to best use them. All my suggestions of things to work on were met with positive responses, but things didn't seem to be progressing; I felt more and more busy, but less and less felt like it got done.

Queue lots of insomnia and mini-panic attacks worrying about making things work. I'd never had lots of issues with that and had started taking some drugs to help get to sleep regularly.

In the end, I had to end the relationship and move on. Partially, so I could start sleep and slough off increasing anxiety, and in part so I could re-focus on building the product and getting customers.

# Pivot 2: Developer Focused Platform

At this point, I came up to my original, arbitrary deadline of January 2017 to get funding. But, winter is a bad time to raise, so I could push that deadline out further. I also knew that I didn't have enough validation and missing a co-founder were hurting my pitch. We were also well past the funding hype of 2015, so turning a profit started looking increasingly important too.

A great piece of advice I had was that perhaps I could turn the challenges around and look at it as a _distribution_ problem and a numbers game to make it at least seem like we had more traction.

Well, many of the people I talked to about what I was building were developers and they _got it_, understanding why it was interesting and novel and hard. Sounds like exactly the kinds of people to attract as users.

Now, rather than going out and talking to a bunch of IoT developers for what exactly they need, I figured that as a developer myself I could certainly know what would work.

It was back to the code cave, this time build out a signup system for users and a UI dashboard so people could actually _see and touch_ what I was building. The initial UI looked good and it was interesting to work with a new technology (even if it was wildly frustrating at times), so my technologist side was satisfied.

At the same time, I also started doing some more 'sales-y' work: scraping together a list of a couple thousand contacts to start cold calling. And I had interest with that, but it was well outside my wheel house and I found it hard to suommon the mental effort to continually persue it.


Once the dev sign up was going, I could actually let some folks on in my beta list with very low friction. Some free users was better than no users, right? And for a little while, it was good. People were using it, finding bugs and I had work that people are interested in having done.

## Data PaaS startups

But there is a core problem in a data PaaS - few companies are willing to give their data to a startup. They can lose it, are probably likely to go down and might not even be around in 6 months.

The only people that can take that risk are startups themselves, but they are rarely going to need to scale benefits of Fineo that are so core to what we offered. So, why trust a startup when I could easily run MongoDB or Postgres for much of my data; at a bigger scale, I could turn to plenty of big companies (Samsung, AT&T, Google) that provide time-series database-as-a-service offerings.

Yes, Fineo has a niche in the SQL features, which fits nicely for folks coming from a traditional database world, but man, it is a big undertaking to change database infrastructure; its months of work to transition and often requires lots of core changes. For most startups, things have to be dire indeed to make that necessary.

# In the end

Nearly two years into Fineo, having run out of network contacts for a potential co-founders and, frankly, being tired and pessimistic about the prospects for _another IoT platform_, I've decided to wind down this endeavor. The struggle to call it quits has been rough: I've spent much of the last 4 months in a deep depression (rivaled only by one or two other episodes in my, admittedly short, life), but still convinced that what I was doing was _novel_ and _interesting_.

I've always been fairly successful at anything I've tried (middle-class white, male priviledge helps a hell of a lot), so deciding to quit has been mentally hard to grasp - I've powered through a marathon on broken legs, sent myself to the hospital to finsh an Ironman; certainly, this isn't the line for me, is it?

# Retrospective

What would I have done differently? A whole hell of a lot. Basically, I did everything completely backwards. Just wanting to start a company and having a couple of indicators you are on the right path isn't enough. Here's the order I'd try next time:

1. Find problem
2. Find companies who have problem
3. Get them to sign a letter of intent for your solution. More is better.
4. Get a cofounder.
	a. Helpful if they are complementary skill-wise, but most importantly someone to completely trust
5. Raise money
6. Quit regular job

For myself, I'm resolving to be more humble, more patient and more outgoing. Starting Fineo has helped me realize - truly, deeply _know_ - how much I don't know and can't do alone. In fact, I suck pretty hard at most parts of this startup thing. And I now get that change, regardless of context, takes a certain amount of time, of buy-in, of pure _hustle_ and sometimes you just need to beat your head against it.

Am I happy with how this all turned out? No, not really.
Would I do a lot of it differently? You bet your ass.
Do I regret having tried? Nope. At least, not most of the time.
Would I do it again? Yeah, I think so.
Soon? Maybe not.

I couldn't have gotten even half way through any of this if it weren't for the comparable support (emotional and business-wise), understanding and patience of my fiance [Megan](https://www.linkedin.com/in/meganliamos/) - a brillant product manager and trancendent baker in her own right. Thank you.