---
layout: post
title: Choosing Hadoop Deployment tooling
location: San Francisco, CA
subtitle:
tags: hadoop, ambari, cloudbreak, deployment, devops, aws, cloud
---

Picking the right tools for deployment can be tricky and have long-lasting effects on your organization. Over at [Fineo](http://www.fineo.io) we have the luxury of doing everything from scratch. This means no concern with legacy tools, monitoring, or just cruft. Instead, we have the opportunity to make all new legacy code :)

At Fineo, we manage a hadoop-focused stack, with multiple layers of dependencies. On top of that, we are providing a multi-tenant, hosted service, so we need to be able to deploy, redeploy, and fluctuate capactity at a moment's notice. Since we are a lean shop, we wanted to write as little code as possible to get up and running as soon as we can. Naturally, we turned to open source!

Fortunately, the great folks at Hortonworks have been doing a lot of the work we need already with [Apache Ambari](http://ambari.apache.org/) - the ability to deploy, configure, orchestrate and manage a stack of services, where most of the sevices we already need (a hadoop stack) are already written for us. Now, all that's left is adding the custom services we need, tuning and actual deployment.

Ambari is great for getting bits on boxes, but doesn't really help us manage our server fleet when running in a cloud-native environment. We want to be able to run [Fineo](http://fineo.io) across multiple data centers and on various different public clouds, depending on where are customers are already running. Suppose you were going to build this all from scratch; you would probably use Docker to manage containers, [Swarm]() to manage the fleet of Docker containers, something like Consul for discovery and configuration services.

Enter [Cloudbreak](http://sequenceiq.com/cloudbreak/). Docker/Swarm/Consul on Ambari with plugins for all the existing major cloud providers. Pretty cool (yay open source!).

Now we have a hadoop-focused stack built on the best in class technology, all without writing a single line of code. That's all about to change.

<img src="/images/posts/choosing-ambari/jenkins.jpg" align="center" alt="Rube-Goldberging our way from source code to deployed bits"/>

## Continuous deployment as Life

Key to getting anything ready for production, is giving developers the tools to replicate a production-like environment locally and test without too much overhead. To do this, I've setup a series of Jenkins jobs linked to maven builds and Vagrant virtual machines. <img src="/images/posts/choosing-ambari/turtles.jpg" height="214" align="right" alt="turtle, turtle, turtle, turtle, elephant, turtle..."> A single check-in causes a cacade that builds brand-new RPM of our custom component(s) (I talk about that [here](/2015/08/21/building-maven-rpms-on-osx.html)), then triggers a build to recreate a VM for hosting those RPMs, alongside a specified verion of the HDP and HDP-UTILS rpm repositories<sup>[1](#hackathon)</sup>. From there, you can run a virtual machine to do a from-scratch install of the whole stack via Ambari. 

That same VM that is used for RPM hosting, with a couple of environment variable changes, can also be used to deploy those RPMS to our S3 repository, which is used for hosting our production, staging and testing environments in the cloud. Oh, and of course you can trigger this via a Jenkins job too.

Vagrant is great in that it gives you exactly the same environment everytime you run it, so we leverage it to also create a Vagrant VM of the Cloudbreak stack (which is actually a VM hosting a Docker instance, which spins up multiple VMs... its turtles all the way down). With that VM we bundle developer credentials so you can then deploy using the existing RPMs or the ones that you pushed to to S3.

This means developers can easy so go from raw source code to a tested, built RPM which they can use to test in a production like deployment, all on their local machine, a mix of jenkins + local machine, a mix of local + cloud or entirely in the cloud. 


## Pros/Cons

Let's go to the high-level list of what I like and don't like about Ambari and Cloudbreak. In the end, the issues were not insurmountable and we trust in the power of a strong open source community to remedy many of these issues.

### Ambari - Pro
 * REST API for everything
 * looking nice UI + extension framework
 * 'stack' based approach, which inheritance, so you don't need to copy-paste everything.
 * Not just Hortonworks stack - BigTop also has its own 'validated' version
 * common components that us you 'just use' when creating your own stack, which covers most of the services you care about

### Ambari - Cons

Many of these issues are due to the relative immaturity of the platform and probably not as big of an issue as if we had decided on Cloudera Manager, but the community support for Ambari is pretty strong, and growing, while the issues are minor at best - RTFS and everything is ok! In the end, they were not insurmmountable and let us get up and running relatively quickly.

 * terrible documentation. The starter project gets you a basic set of scripts deployed, but skips deploying an RPM package. There is no real documentation on other options or usages.
 * Error messages are obtuse - spent days debugging a messed up configuration issue
 * The code is hard to follow, so debugging is a bit of a pain. Probably my lack of experience with the codebase, more than anything though. Would be nice to have any kind of component diagram.
 * UI still immature - installation steps are somewhat opaque (which can be good sometimes too)
 * stack inheritance can make things really hard to understand, if you get more than 2 layers deep
 * you can't do cross-stack inheritance
 * still very HDP oriented

### Cloudbreak

I'll be honest, we still haven't done very much with Cloudbreak - its still very early days. However, the platform does everything that I would have designed alreday, so I can't complain too much. 

#### Pros

 * All the 'cool' technology of next-gen devops - Docker, Swarm, Consul
 * Great looking web UI
 * Nice shell and exposed API for our own tooling
 * Pluggable cloud APIs with existing support for all the major clouds
 * Recent acquisition by Hortonworks indicates continued suppport and development (maybe apache project??)
 * Great existing support on forums, mailing lists (they already rolled their own AMIs for an easy start!)

#### Cons

 * Local deployment guide could use a little bit of work
 * Needs [some tricks](https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/cloudbreak/6LIeYW6mnL8/j8F8y8enBgAJ) to run in Vagrant (though this is really a Docker bug)
 * Requires a bunch of microservices, which can make setup/management a bit of a pain for newbies 

Like I said, this is still early days - I'm sure other things will come up as we use Cloudbreak more. However, great support and signalling for increased development and support via Hortonworks, combined with the open source nature of the project bodes very well for cloudbreak. 

# Summary
Devops - deployment, orchestration, management - is hard, particularly for the hadoop stack. Luckily, people have been doing a lot of _open source_ work on this already, which lets us jump-start our own process with just a bit of elbow grease and source code reading. [Ambari](http://apache.ambari.org) is a great platform for deploying, configuring and managing your hadoop cluster - that's months of development time saved!

As a green-field, 'cloud-native', multi-tenant platform we have to be able to deploy to multiple clouds and datacenters, as well as respond dynamically to shifting load requirements. Enter [Cloudbreak](http://sequenceiq.com/cloudbreak) - cloud-native deployment built on Docker and Swarm (that's another 6 months of hacking saved, but with a tested, relatively 'solid' platform) that manages clusters through Ambari.

[Fineo](http://fineo.io) is still young, so we haven't seen all the rough edges that Ambari and Cloudbreak will have, but we are jumping in head first, trusting to a strong open source community to move these projects along quickly. There is a sharp learning curve, but they have proven to be a powerful springboard to get our hadoop-based stack running.

Combined with Vagrant and a few continuous integration jobs, we built a full development pipeline that is amenable to local testing, cloud-testing or production deployment. With the invaluable APIs exposed by Cloudbreak and Ambari, we can build our completley automated infrastructure and a true CI environment - a true rarirty in the 'big data' world!

At Fineo we belive that the Internet of Things means that every company has to become a big data company. Unfortunately, big data tools are still hard to use (even with all these great tools!) - Fineo brings big data to enable IoT for the rest of the world, backed by a world-class platform built on tools like Cloudbreak and Ambari.

If you are interested in solving these kinds of problems or bringing big data and IoT to the world, <a href="mailto:ceo@fineo.io?Subject=Let's bring big data to the world!" target="_top">drop me a line</a>.


Footnotes
==========
<a name="hackathon">#1</a> Speaking to the power of open source, at the recent Ambari Hackathon, the folks from Pivotal made an [extension to allow additional repositories](https://twitter.com/2twitme/status/655572585837559808). This will let us then separate out the external dependencies (which we host locally, for full control, managment, access, etc) and internal service RPMs.

Credits
=======
Turtles all the way down: [Lynn Blog: June 2010](https://www.google.com/url?sa=i&rct=j&q=&esrc=s&source=images&cd=&cad=rja&uact=8&ved=0CAYQjB1qFQoTCKjwrpPt4MgCFQF1JgodEKYAog&url=http%3A%2F%2Flynnxedinny.blogspot.com%2F2010_06_01_archive.html&psig=AFQjCNG5XrBhYy9QG2sPoRLXNboj_LDTzA&ust=1445973365345336)
