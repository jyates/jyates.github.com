---
layout: post
title: Alexa as an API makes smart homes a reality
subtitle: 
location: San Francisco, CA
tags: smart home automation, alexa, amazon echo
---

For years, hobbyist having been hacking their homes to create smarter parts that respond to their every whim. Smart homes were thrust even further into the public concience with the first Iron Man (2008) movie. Suddenly, everyone wanted self tinting, weather informing windows or a home assistant smart enough to do their job for them (or at least turn up the music).

In the last six years, we have come a long way to fulfilling that vision, with smart [thermostats], [lights], [blinds], the list goes on. However, each of these things operates on its own, requiring you to learn a new interface, a new set of commands, not to mention the sometimes onerous installation process. This has left the true automated homes still out of reach and weak semblances in their place, accessible only to those people willing to brave the cutting edge.

It should not be this hard.

# Alexa and Amazon Echo

One of the most interesting new pieces of technology that has mostly slipped under the public's radar is the [Amazon Echo] - a remarkably smart tube you put in the middle of your living space. With some of the best voice reconigition and AI we have seen, certainly rivaling Google Now and far surpassing Apple's Siri, it could be the fulcrum around which the Internet of Things (IoT) revolution pivots to enable smart homes for the masses. A recent [Exponent podcast] called out the Echo as being a brilliant play by Amazon to become the hub of the smart home, the driver for everything else, by selling this at-cost little column that has _just enough functionality_, but is positioned as a tool, an enabler, of the rest of the ecosystem (hint: much of AWS is built around the [same idea]).

Right now the Echo, queried with the name "Alexa", is still somewhat limited in functionality. But at $100, its a pretty compelling piece of tech for most nerds. If you are already invested in some other smart homes and know about [IFTTT], you can add voice commands to Alexa that control things like you Nest or Hue lights; it takes a little work, but it can be done by those willing to google a little.

# Other Home Hubs

This play for the "smart home hub" is not new - Google is trying to pull it off with the Nest and Apple seemingly with the AppleTV. However, both fall short for their own reasons.

## Nest

The Nest is a cool device and many people now have them, but the additional ecosystem they are building is around more devices connected to the Nest and driven from _your phone_. For things like their camera, this makes complete sense - you will monitor it from your phone when you aren't at home. 

But what is the one place you aren't nearly as likely to have your phone? At home.

So then you are back to non-on-demand actions - presets and learned functions for your devices, rather than easy, at-will control. Don't get me wrong, it is very much the _right thing_ for devices to learn what they should do, rather than having you tell them. However, humans are notoriously capricious, and need a way to change it _right now_.

Google could turn this around pretty fast by embedding a microphone and Google Now into their thermostats. Now, you have many of the same capabilities you get with Alexa, but its also integrated into the rest of the ecosystem Nest has been building up. I wonder though if Google can make this happen - they are very focused on search, which is the not at all what you are doing with voice commands; instead, **you ask for what you want**.

## AppleTV

The AppleTV is a weird product. Its meant to be a home entertainment hub, but doesn't play well with anyone else, and then has problems when you are interrupted while AirPlaying. It inherently is neglecting the new paradigm of multiple screen entertainment. For example, when watching a sportsball game, people are also live tweeting the game and checking facebook and messaging with their friends (I know, kids these days).

Futher, you have to go through iTunes, which quite honestly, sucks. I only use it as a last resort to get to content. In fact, I'm much more likely to torrent something than I am to use iTunes becuase it is hard to figure out where things are, even if I would gladly pay for it. This is a scary proposition for something that wants to be the new hub of entertainment. 

### Remotes suck

Remotes were fine when we just wanted to change channels. They were ok when we also wanted to watch VHS movies. Once we started trying to navigate DVD menus, remotes started to suck. Then we got smart TVs and remotes really and truly started to suck.

Apple hasn't really done anything to fix that. The AppleTV remote adds a touch pad, but we are still pointing a thing at the TV and trying to drive a screen feet away from us.

I'm convinced that screens more than a couple of feet from our faces are inherently harder to use; just think about trying to navigate a mirror of your desktop when you hook up to a presentation... it feels a hundred times harder and _its exactly the same of the screen you use every day!_

Recently, [Vizio had an opportunity to redefine their TV experience] and decided to completely ditch the remote. Instead, the remote is a standard Android tablet they package with the TV and you control everything on the TV through Chromecast.

### Yes, yes, oh god yes

Chromecast is brilliant. You do all your searching on your phone or on your computer and then get the Chromecast to go to the same URL and stream from there (in most cases, but you can also stream your exact screen over the WiFi). The local screen is the ideal place for search, rather than trying to wave a remote at a screen.

Chromecast then becomes a much more natural implementation of the entertainment center and the phone as our means to find that entertainment.

And what do we have with us all the time? Our phones.

... except in the home.

Vizio gets around this by getting us to develop new habits around replacing the tablet 'remote control' with some pretty smart psychological hints.

## Smart watches to the rescue

The only thing that we are less likely to take off than our phone is the smart watch. Right now, they are kinda useless devices. Yes, you can see/dismiss/auto-respond to texts and take calls on speaker and track your activity, but that is only on the nicest ones. I've found most people are interested mostly in the health tracking information and often find the rest somewhat annoying. Lastly, many of them are ugly; there are a couple that look OK, but nothing that is truly good.

Right now, smart watch battery life is a bit weak, so we are still taking them off regularly to charge. However, this we are still in the early days and will only see that improve. Further, some people are working on truly wireless charging (so you can be anywhere in the home and charge your phone), so we will soon see smart watches worn just likely regular watches, especially as they become aesthetically as pleasing.

## Alexa as an API

Amazon is am API company and the Echo is very little more than a light frontend for the Alexa API. What is interesting is that Amazon is currently making [Alexa available to developers], so it can be embedded in devices. 

Wait a second. Couldn't we put that same voice recognition smarts into our watches? Now we don't need to be anywhere near the Echo - in fact, it will always be on us - to drive all of our home automation. Search will still reign supreme on the screen, but the watch will soon be the remote control for the rest of our life.

And suddenly that smart watch starts to make a whole lot more sense... and lets be honest, Amazon was never much of a hardware company.

# Closing the loop

We can use our voice to get what we want and a screen to find the things we didn't know we wanted. 

So, what's left?

Well, we still have all these different 'smart' devices connected. When each one comes on, it has to slowly learn your habits and is driven from a device - right now, your phone, later on demand by your voice-over-smart-watch. Wouldn't it make more sense if there was some central hub all into which all these devices connected? Or, so you don't need to take over the world, a common protocol for exchanging information, which could lead to a marketplace of hubs.

Then when you buy those smart blinds, they can find out from your thermostat when you get up and can lower the shades when the thermostat finds its getting too hot (rather than turning on the AC).

All the manufacturers need to do is implement the protocol and you pick the hub to which the device connects. From there, we can enable manufacturers to monitor their devices as much as you desire (not at all, minimal functioning, full data) so they can do proactive maintenance, make suggestions and build better products.

To quote William Gibson, "the future is here, it's just unevenly distributed".

[thermostats]: http://www.nest.com
[lights]: http://www2.meethue.com/ 
[blinds]: http://www.mysmartblinds.com/
[Amazon Echo]: www.amazon.com/echo
[Exponent podcast]: http://exponent.fm/episode-070-is-that-an-echo/
[same idea]: http://www.allthingsdistributed.com/2016/03/10-lessons-from-10-years-of-aws.html
[IFTTT]: https://ifttt.com/
[Vizio had an opportunity to redefine their TV experience]: http://www.theverge.com/2016/3/22/11279954/vizio-smart-tv-google-cast-tablet-remote-smartcast-app-feature
[Alexa available to developers]: https://developer.amazon.com/public/solutions/alexa/alexa-voice-service
