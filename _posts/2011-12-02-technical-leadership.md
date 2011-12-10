---
layout: post
title: Technical Leadership
---

# Technical Leadership
December 2, 2011 - Seattle, WA

Yesterday I was talking with a potential future boss and was talking about what I was looking for in my next position. One of the things I mentioned was I wanted the a technical leadership role. Now, to someone already in managment who has been around for a while will naturally be a little suspicious - as well he should be; the very first thing he asked me was, "So why do you want to be a lead? Because you then have power to boss people around?" 

Don't trust someone who wants power until you understand the why. I've found the best leaders are rarely leaders by choice, but rather because there is no one else better around. Being a leader is stressful, hard, and often times filled with minutae not immediately related to the project. In short, it can really suck. 

So why in the world would people want to be leader? When it comes down to it, it really means control. Control over the vision, the execution and the final product. It is the chance to fix the problems that you have seen in previous projects. It's the chance to nurture your developers and watch them grow. Its about getting to that feeling of project 'flow' and knowing you enabled. But in the end, it is the chance to build something bigger than you can alone - having a vision and building something impactful from scratch. 

You can be a technical leader and embrace all the pain and the joy of building great software or you can just technically be the leader and with some certainty just crank out crap and make everyone (including you!) unhappy. Just think about all the people who build great software - you have to jump in head first or you are going to crash and burn.

Coming from being a software developer, assuming technical leadership really is just a higher level abstraction - you have to think about the whole software ecosystem: the people building it, the architecture, the clients, the goals. Which means yes, you have to have authority to 'boss people around'. Otherwise, you cannot orchestrate (and I chose that word intentionally) the entire system to suceed, to reach its full potentially. If you don't have any dejure power, it can take years to build up enough repoire and cache to build great things. Sometimes you don't have years - it needs to get down _now_. In that case, having a boss hand down authority certainly helps. Yes, it doesn't work if the people under you don't respect you and believe in you, but those are basic qualities to any good team. Once the leader-follower dynamics are estabilshed, it becomes much easier to actually guide the work, rather than wringing your hands and waiting. (Full disclosure, some of this comes from some experience having to lead a team from an unofficial position - its a very nasty situation which leads to lots of frustration and roadblocks. Having the dynamic established is criticial to moving quickly.).

But with great power comes great responsibility.

That responsiblity is to building a great product. What does that mean? The first is responsibility to your team. Its becomes your job to enable them to succeed. That entails a multitude of things: providing cover from "bs" work, making sure people have work to do, ensuring the project is synchronized, that right people are working on the right things and ensuring that all their hard work gets communicated back up to managment. Making it easy for your team to suceed makes it easier for them to build something great. This also means helping where necessary and (this is harder) stepping back and letting the really smart people just work. 

If everything is good, the team should feel like its floating, it just easy. It just works. There are no major hitches. Everything is clicking with the developers. Work is being getting done on sprint boundaries (+/- technical difficulties). But this is a hard thing to get to, and there are a lot of tools and books out there to help leader just let developers write code (agile, xp, cms, etc.), so clearly this isn't a solved problem. Hey, writing software is a craft not engineering (nor did anyone say it was easy). 

There are risks in any project and things can fall behind because of unexpected delays. If that's the case, you shouldn't crack the whip - that will only break spirits over time, though it may get _this_ project done - but instead make sure everyone (the team, management, the client) understands what's going on and why this happened. Its all about communication, about making sure everyone knows what's happening with the project. If its behind because people are slacking then, by all means, start raising some hell (politely, of course), but that is not the general recourse.

The worst thing that could happen is people thinking that everythink is hunky dory, when in fact they are going off the rails. This leads to people being angry, a shoddy product, an overworked team, or some combination of the previous. 

As a lead, your main job is really to facilitate communcation. Make sure the developers are talking and coordinated. Make sure the client knowns whats happening. Make sure upper management is apprised of the project status. No suprises. This is your moat - the first line of defense against failure.

In this however, you have to be the conduit between the developers and the client and the management. If the managers start bothering the developers or the client pesters them all the time, they won't be able to do what they do best - write code. This often means taking the boring tasks like writing powerpoint slides, long meetings, and extra-extra documentation (though the severity of all this depends on the size of the company. At smaller companies much of this pain will be gone.). This doesn't mean the developers shouldn't be able to talk to the managers or client if needed, but that they should do it only when they need to - not right in the middle of working through some really tough, gnarly code that requires an hour to even get in the right frame of mind to work on (or more concisely, needs 'flow'). By providing that buffer, you can keep the developers happy, which keeps everyone else up the stack happy too.

However, there are a couple things people can encounter to make it seem like they are slacking:
* black holes - a piece of the architecture that no one knows that much about, requires some investigation to complete, and is important to the system
* worm holes - even worse than black holes, wormholes take you into not just the product you are working on, but into the internals of two or three (I hope its not more) layers deep - into other projects - to fix an internal bug in their code.

Black holes are obviously bad - they can be a huge time sink and lead to schedules longer than the amount of time in the universe (see [Dreaming in Code](http://www.dreamingincode.com/)). Wormholes are something I recently stared using after spending about 2 weeks to find a workaround for a bug that should have been fixable in hours, in the process uncovering bugs in another component of the system as well as two crucial ones in the database we were using.

In cases like this, it is important to try to mitigate those risks by lending as much guidance as possible. As the tech lead, you need to have seen lots of problems, worked through a bunch of them yourself, know when to tear it out and start again, and (possibly most importantly) know when to file a bug and move on. If you don't mitigate these risks, the whole project starts to fail - people get frustrated, the excitement goes away - you quicly go from floating to falling.

At the same time, its likely you have some junior developers on the team (you do, right? if not, get some - what happens when the senior guys leave?). If so, what may be a black hole for them, is really simple for one of the experienced developers. It's all about figuring out how much help people need and if you or another team member need to step in for support. Start with small questions (any problems today?), and then if things seem fishy start to escalate until you get to the root of the problem (its all just communication!). At that point, maybe you step in and more closely monitor their work or maybe do a some pair programming (or set some up with an experienced member) or 'realign' them to something more suited to their skills. Its suprisingly frequent to find that people are just tasked with the wrong thing; they may seem completely incompetent because it doesn't match the way they think, but if you find the right thing they can just fly. So a couple different things to think about there, a couple different options for help - if the team is open enough, the problems become apparent quickly, making the solutions that much easier.

So we've talked about the responsilibities (and there is no lack of them). Now let's talk about why you would even want to be a technical lead. At least for me, its all about what I was talking about originally - building something bigger than you could alone. Big doesn't just mean a lot of lines of code, there are implications for the complexity and the usefulness of the software at the end. Honestly, who doesn't want to engineer something that no one else has ever done that makes a huge impact in the world? In fact, that's the premise of most startups.

In building something big, as a leader providing the vision is as important as shaping the product around the vision and the vision to the product. Chances are, what you intended to build in the beginning isn't really what you have in the end - it may be close, but at you build it, you learn new things - what works, what doesn't, if it has impact, etc. If you don't adapt to what you have learned, you are shooting yourself in the foot and will probably end up with a pile of junk. That's not to say you should radically change the plan every week (though 180s are necessary from time to time), but that change needs to be tempered.

In the end, it's all about taking that vision you have, getting people excited about that idea, and building your own software castle in the sky. 

To recap, if you want to lead well (as least as far as devs are concerned), you just need to provide:
 1. vision and direction for the project
 2. guidance when people need it
 3. clear channels of communication (and ensure they are used)
 4. cover so your developers can do what they do best.

Yeah, its not an easy job - lots of stress from above and below, worries about schedules, concerns over providing direction and leadership, and all in hopes of turning your dream into reality. So go on and build your castles - revel in the pain, the work, and take some time to enjoy what you've accomplished...

Then I'm moving onto the next big thing because for me the joy is in building, not sightseeing.
