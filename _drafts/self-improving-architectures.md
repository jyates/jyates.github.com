---
layout: post
title: Self-Improving Architecture and Design
tags: startup, design, architecture
---
designing systems for actors to act in the best interest and best interest of sustaining the system. eg. can you dev a codebase/env that is in the (short term?) self interest to improve the mechanisms? see [exponent #110 - 10:45](http://exponent.fm/episode-110-moral-hazard/)

From exponent, seen in the historical democratic system in the US, circa 1930, concern was over ability to maintain democracy (partitioning radio air space so it couldn't be monopolized). With the advent of the changes - new tech in radio - we not only preserved the state of the system, but actually sought to improve the system through changes and it _was the easiest way to make the changes_.

Education: improving the school system by the design of the school system - leadership growth internally to grow more leadership. [link](https://www.ascl.org.uk/download.E75C72C0-1D05-4C8B-9F4CA40CE09B8D8B.html).

Software: classically, much of machine learning when deriving a best solution. However, also seen in [designing predictive and reactive architecture management], this is more about how can we design the system to improve itself while running (e.g. feedback loop for real-life against an objective success function).

When building organizations and software, changes are often most easily made in ways that degrade the system - "just a X to do the job" - until you end up with a big ball of spagetti. Unless you are explicitly managing the _entropy_ of the system, it approaches chaos.

In fact, looking at entropy as a general theory, there might be evidence that tending towards cohesion (away from chaos) is infact impossible without the constant application of outside pressures/forces.

Much like a child, if not told to clean up their room, the toys will often end up strewn across the floor. As you grow older, however, one learns the value in developing a system of organization and returning things to their original place. Again, the application of outside force (your effort) moves the room from chaos to order.

Looking at that example, we can see a separate organizational structure at play that drove the _right choice for the system_ even though it was more effort: the conditioning from your parents. By teaching you (imposing a mental framework) to seek an organization because the energy cost long term is lower its easier to consistently find the thing you want.

There may be hints of this in [holocracy](http://www.holacracy.org/how-it-works/), most well known as the [organizational structure at Zappos](https://www.zapposinsights.com/about/holacracy), where the structure of the organization pushes the responsibility for changes to the 'edge' - to the people most in tune with the local requirements - where it can do the locally optimal thing. The understanding then of the most optimal choice comes from the 'top' of the company which set the strategic goals that inform the the tactical choices made at the edge.

For an organization focused on innovation, the strategic goals should then be set around what _outcomes the customer is trying to achieve_ and how well satisfied are those outcomes. Then its a question of just making what your customers want. However, if you leave the innovation up to a small number of individuals - e.g. a think tank - you are inevitably going to miss lots of opportunity.

In fact, that is exactly what Ben Thompson in that episode of Exponent mentioned above get at with the inevitable stagnation of a centrally planned economy; there are just too many possible routes and options (essentially infinite), making it impossible for a small group to optimize. Instead, we should allow a general market to determine the best choices via awarding dollars to the success(1). Given that people will continually pursue things in their own personal best interest, the challenge then is to make risk/downside relatively small while encouraging a large upside.

Within a single company, of course you cannot pursue every possible idea for innovation someone within the company dreams up. They have to be evaluated against the those strategic goals that were derived from what the customer wants; essentially asking, "how well is this going to satisfy the customer?" Fortunately, this also provides a framework within folks can generate new ideas (often a boon to the ideation process). Within a holocracy, people at the edge are then empowered to pursue these goals, proving them lots of potential reward (intangible, like seeing your idea become real, or tangible like bonuses, promotions and a rising stock value).

The risk for failure of a change is minimal for the implementors as management has to accept that they not longer have 'control' over the tactical things that occur. However, folks still need to be penalized for things not within the strategic vision of satisfying the customer. If an initiative fails, it then needs to become a lesson in understanding of what failed (i.e. [the five whys](https://en.wikipedia.org/wiki/5_Whys)), be in internal processes, market changes or imperfect understanding of the customer needs. In fact, these failures should be embraced because they provide the basis for a better approach.

And really that all comes back to a lot of the what Lean Startup get at with attempting to understand the customer, trying, failing and iterating. Keep in mind, following this process at an established company does not mean that you are 'safe' from disruption. Instead, its merely that you avail yourself of more sources of targeted innovation. A disruptive innovation could be used to grow into new markets, while a 'sustaining' innovation increases the value of your product for customers and helping grow market share.

## Software Architecture

There is an interesting question as to whether a self-improving/continually improving codebase can be developed. From the CS example above, the state of the system can be improved, but can the continual development continue to improve the system? In short, can you make a codebase wherein the developers best choice is to make the codebase _better_?

A simple approach might be tying job success metrics to code base improvements changes, i.e. 'boy/girl scout points' for leaving it cleaner than you found it. Its then the _organization_ and not the code that forces the improvement. This could drive innovation around improving the architecture such that the code is easier to maintain and understand (arguably more important, in all but a few situations, than performance).

Amazon seems to have a hint here as well with maintaining separate service groups where the goal is _zero meetings_, instead using well defined APIs to communicate. This architecture lets teams innovate internally, with minimal friction, while driving towards pleasing customers. Because codebases are phyiscally separated they are less likely to become 'infected' with outside influence (and generally an argument to modularization).

However, if we look to startups, the key goal for the software stack is to get to something we can use to validate the hypothesis for pleasing the customer _as fast as possible_. Then, code quality has historically taken a back seat to 'done'. Naturally, this can degrade quickly into a nearly un-maintainable mess, which has to painstakingly - module-by-module - be rebuilt into something manageable.

Maybe in taking a note from the [designing predictive and reactive architecture management] we can build in core systems that look at potential costs of change, or even look at trends in development, to determine what facet of implementation you should focus upon. Naturally, the granularity here needs to be managed - you wouldn't want to thrash between refactoring and changes every day.

# Wrap Up

The idea of a system within which its actors are driven to improve the system is powerful. A key understanding of the actors then must be the metrics of success for the system. In a startup, this could be number of new users or the amount of customer churn. For a democracy, this could be focusing on the availability of information. For capitalism, this could be amount of risk/reward for a new venture. Of course, many of these systems intertwine, so its then incumbent upon everyone to want think about (a) what the goal of the system is and, (b) what metrics are used to achieve those goals. With that understanding, its becomes much more likely for folks to come up with the 'right' ideas and understand what makes those ideas correct. 

To paraphrase from Ben Thompson, anything taken to its extreme inherently becomes no longer that thing. When thinking about the 'right' thing, perhaps we also need to keep in mind that everything should be taken in moderation... including moderation.

## Notes

(1) This is not to say we need to be in an Ayn Rand fantasy world. Having a social safety net is good, having regulation around business practices to prevent harm (e.g. not polluting or having safe working conditions) are good.

[designing predictive and reactive architecture management]: http://repository.cmu.edu/cgi/viewcontent.cgi?article=1652&context=compsci)