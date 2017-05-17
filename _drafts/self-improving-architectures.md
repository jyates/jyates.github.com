---
layout: post
title: Self-Improving Architecture and Design
tags: startup, design, architecture
---

The idea of a self-improving system where the actors in and around the system are incentivized to continually improve the system is very enticing. I wanted to look at can this be extending to a code base? Or within a company? I want architecture of the system to dictate the _lowest cost/lowest energy_ action to take within the system is also the _best_ choice for the system. This exploration was sparked by a recent episode of the Exponent podcast (specifically: [exponent #110 - 10:45](http://exponent.fm/episode-110-moral-hazard/)) discussed the conditions around how democracy in the 1930's was incentivized to continue to improve democracy; it was in the best interest of the politicians (actors within the system) to continue to safeguard or improve the core system of democracy in the face of new technology.

The concern was over ability to maintain democracy when considering the rules for partitioning radio air space so it couldn't be monopolized. A monopoly on the airwaves in a region would allow one entity to control the flow of information, effectively hamstringing the democratic processes.  With the advent of the changes - new tech in radio - we not only preserved the state of the system, but actually sought to improve the system through changes and it _was the easiest way to make the changes_ (otherwise they would have been voted out of office, or so they thought).

The system was seen to break down when partitioning the TV frequencies. Instead of information flow being the metric of success, profits become the considered metric. With the shift from the underlying metric of value to a proxy, we see a shift in outcomes, to preserving the structures for existing companies to drive more profits.

For many, capitalism and American Democracy are, if not one and the same, at least closely interwoven. On the micro-scale, the goal of capitalism for a single individual is to reap large profits. However, at the macro-scale, capitalism is about maintaining a large number of competitors wherein the best solution for customers gets the rewards, further encouraging innovation and growth to capture customer dollars. "Natural" capitalism is then a self-improving system where the reward of profits drives the creation of enterprises (1).

# Self-Improving systems across industries

Self-improving systems are not a new idea and is seen across many industries. In education some have proposed designing the school system to grow leadership to drive more leadership across the different facets of the system [link](https://www.ascl.org.uk/download.E75C72C0-1D05-4C8B-9F4CA40CE09B8D8B.html).

To many folks, the first thought of a 'self improving system' is to think of machine learning. In many applications, the goal is develop an algorithm/system that learns by its nature (even if we don't fully understand it) increasingly good solutions(2).

There is also an interesting example from [designing predictive and reactive architecture management], where they develop a feedback system that looks to optimize a set of goals based on a set of input metrics. In this case it's more about how can we design the system to improve itself while running (e.g. feedback loop for real-life against an objective success function).

## Self-Improving tech organizations

When building organizations and software, changes are often most easily made in ways that degrade the system - "just a X to do the job" - until you end up with a big ball of spaghetti. Unless you are explicitly managing the _entropy_ of the system, it approaches chaos.

In fact, looking at entropy as a general theory for dissecting this problem, there might be evidence that tending towards cohesion (away from chaos) is impossible without the constant application of outside pressures/forces.

Much like a child, if not told to clean up their room, the toys will often end up strewn across the floor. As you grow older, however, one learns the value in developing a system of organization and returning things to their original place. Again, the application of outside force (your effort) moves the room from chaos to order because the long-term energy expenditure is worth the short-term expenditure (aesthetics aside).

Looking at that example, we can see a separate organizational structure at play that drove the _right choice for the system_ even though it was more effort: the conditioning from your parents. By teaching you (imposing a mental framework) to seek an organization because the energy cost long term is lower its easier to consistently find the thing you want.

There may be hints of this in [holocracy](http://www.holacracy.org/how-it-works/), most well known as the [organizational structure at Zappos](https://www.zapposinsights.com/about/holacracy), where the structure of the organization pushes the responsibility for changes to the 'edge' - to the people most in tune with the local requirements - where it can do the locally optimal thing. The understanding then of the most optimal choice comes from the 'top' of the company which set the strategic goals that inform the the tactical choices made at the edge.

For an organization focused on innovation, the strategic goals should then be set around what _outcomes the customer is trying to achieve_ and how well satisfied are those outcomes. Then its a question of just making what your customers want. However, if you leave the innovation up to a small number of individuals - e.g. a think tank - you are inevitably going to miss lots of opportunity.

In fact, that is exactly what Ben Thompson in that episode of Exponent mentioned above gets at with the inevitable stagnation of a centrally planned economy; there are just too many possible routes and options (essentially infinite), making it impossible for a small group to optimize. Instead, we should allow a general market to determine the best choices via awarding dollars to the success. Given that people will continually pursue things in their own personal best interest, the challenge then is to make **risk/downside relatively small while encouraging a large upside**.

### Balancing Risk/Reward with Strategic Goals

Within a single company, of course you cannot pursue every possible idea for innovation someone within the company dreams up. They have to be evaluated against the those strategic goals that were derived from what the customer wants; essentially asking, "how well is this going to satisfy the customer?" Fortunately, this also provides a framework within folks can generate new ideas (often a boon to the ideation process - adding constraints _helps_ creativity). Within a holocracy, people at the edge are then empowered to pursue these goals, proving them lots of potential reward (intangible, like seeing your idea become real, or tangible like bonuses, promotions and a rising stock value).

The risk for failure of a change is minimal for the implementors as management has to accept that they not longer have 'control' over the tactical things that occur. However, folks still need to be penalized for things not within the strategic vision of satisfying the customer. If an initiative fails, it then needs to become a lesson in understanding of what failed (i.e. [the five whys](https://en.wikipedia.org/wiki/5_Whys)), be in internal processes, market changes or imperfect understanding of the customer needs. In fact, these failures should be embraced because they provide the basis for a better approach.

And really that all comes back to a lot of the what Lean Startup get at with attempting to understand the customer, trying, failing and iterating. Keep in mind, following this process at an established company does not mean that you are 'safe' from disruption. Instead, its merely that you avail yourself of more sources of targeted innovation. A disruptive innovation could be used to grow into new markets, while a 'sustaining' innovation increases the value of your product for customers and helping grow market share.

## Software Architecture

There is an interesting question as to whether a self-improving/continually improving codebase can be developed. From the systems example above, the state of the running system can be improved, but can the continual development continue to improve the system? In short, can you make a codebase wherein the developers best choice is to make the codebase _better_?

A simple approach might be tying job success metrics to code base improvements changes, i.e. 'scout points' for leaving it cleaner than you found it. Its then the _organization_ and not the code that forces the improvement. This could drive innovation around improving the architecture such that the code is easier to maintain and understand (arguably more important, in all but a few situations, than performance).

Amazon seems to have a hint here as well with maintaining separate service groups where the goal is _zero meetings_, instead using well defined APIs to communicate. This architecture lets teams innovate internally, with minimal friction, while driving towards pleasing customers. Because codebases are physically separated they are less likely to become 'infected' with outside influence (and generally an argument to modularization).

However, if we look to startups, the key goal for the software stack is to get to something we can use to validate the hypothesis for pleasing the customer _as fast as possible_. Then, code quality has historically taken a back seat to 'done'. Naturally, this can degrade quickly into a nearly un-maintainable mess, which has to painstakingly - module-by-module - be rebuilt into something manageable.

Maybe in taking a note from the [designing predictive and reactive architecture management] we can build in core systems that look at potential costs of change, or even look at trends in development, to determine what facet of implementation you should focus upon. Naturally, the granularity here needs to be managed - you wouldn't want to thrash between refactoring and changes every day. Again, it comes back to what metrics of success are used and having the right gauges to understand the influence on those metrics.

# Wrap Up

The idea of a system within which its actors are driven to improve the system is powerful. A key understanding of the actors then must be the metrics of success for the system to adjust actions for the those goals. In a startup, this could be number of new users or the amount of customer churn. For a democracy, this could be focusing on the availability of information. For capitalism, this could be amount of risk/reward for a new venture. 

Of course, many of these systems intertwine, so its then incumbent upon everyone to want think about (a) what the goal of the system is and, (b) what metrics are used to achieve those goals. With that understanding, its becomes much more likely for folks to come up with the 'right' ideas and understand what makes those ideas correct. 

To paraphrase from Ben Thompson, _anything taken to its extreme inherently becomes no longer that thing_. For instance, democracy to its extreme is mob rule. However, when looking at the extreme of the _goals_ of democracy we see fair governing of a people based on their needs and desires (something that inherently doesn't look like mob rule).

So maybe it comes down to keeping in mind the right metrics of success, not the existing system within which are operating. Any making those end goals understood across the organization. 

Maybe? Still working on this one...

## Notes

(1) This is not to say we need to be in an Ayn Rand fantasy world. Having a social safety net is good, having regulation around business practices to prevent harm (e.g. not polluting or having safe working conditions) are good.

(2) Please for forgive the overly simplified view of ML. I know, its a lot more complicated.

[designing predictive and reactive architecture management]: http://repository.cmu.edu/cgi/viewcontent.cgi?article=1652&context=compsci)