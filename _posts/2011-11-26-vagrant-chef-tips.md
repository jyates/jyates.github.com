---
layout: post
title: Vagrant + Chef - Tips and Tricks
---

# Vagrant + Chef - Tips and Tricks

November 26, 2011 - Somewhere over the continental US

There is a good chance you got here looking for a solution to some fincky problems with vagrant and/or chef. Congrats on using some cool tools! If not, I'm going to introduce the tech I'm talking about and why you should even care. THEN onto the meat - some 'interesting features' of each you might need to keep in mind.

## Chef 

Imagine that you are expanding your Hadoop cluster and want to add another data node. Well, that means you are going to have to install java, download the correct hadoop version, update the configuration files, and startup the datanode process. That can be a bit of a pain for your sys-admins, though with practice you can probably get it down to 5-10 minutes of scping and fiddling. What if you could do it with a push of a button and you _know its exactly the same_ as every other datanode in your cluster?  

Or what if you need to replace your Job Tracker? Push a button. 

Or your web server goes down? Push a button.

If you haven't heard of it by now, Chef is one of the easiest ways (feel free to rant about the qualities of [CFEngine] or [Puppet] in the comments) to manage the configuration of your computers, particularly in a cluster (though there are certain places where they really aren't 'cluster aware', see [Umbria]). There a a bunch of tools out there to manage this, but they all follow the same basic idea.

You store the configuration of each 'role' on some remote server. Then when you fire up a new machine, you point it at that server it downloads the configuration it 'should' look like, and then tries to build itself up to that configuration. 

Chef is nice because it is all in Ruby (and a special, Ruby-like DSL). This means anything you can do in Ruby, you can do in Chef. This means is is really easy and natural to do more complex configurations. There is also a pretty strong (and growing) community around Chef, with tons of people open sourcing their own Cookbooks. The short story of that is, you can get up and running in minutes AND you have great examples of how to write your own cookbooks.

If you want to learn more about Chef, I would recommened [this tutorial], the [official wiki] (which is pretty dang good), and this [terms guide] to help keep your head on straight).To people not used to these systems, it can be a lot to wrap your head around (it was at first for me too), but once you understand the paradigm, its simple to write your own recipes and leverage the others out there.

There are also a couple of ways you can try out chef. First, you could run your own chef server. Its a little daunting, to jump to that immediately (though the offical wiki has some great info on how to do that). Next easiest step is Opscode's option to try out 'Hosted Chef', where they run the configuration sever for you and (even better) its free to try on up to 5 nodes (aweome!). The last thing you can also try is to use Vagrant. Vagrant is a tool to dynamically build virtual machines; the dynamic part comes from using Chef to configure the VM. No remote server and minimal configuration pain (though there is some 'fun' associated with it). 

So there really isn't any reason not to try chef, if you haven't already. 

## Vagrant

Ever wanted to ...

The official [Vagrant tutorial] is actually pretty fantastic. It's definitely your first stop in getting the system up and running. Go ahead, try it. Work through the examples.  

Buuuuut, it doesn't have everything you say? Fair enough, your problems (I'm guessing) are probably stemming from chef. Check out this great [blog post] about how to use chef+vagrant together.

## Tips and Trick (what you've all been waiting for)

There are some facets of the interaction of vagrant and chef that can cause problems. Hopefully, the resources above answered your questions, but if not, lets dig into some of the things I found.

### Setting up your directories (and version control _everything)

Keep a separate site-cookbooks for your own cookbooks. Its mentioned in the chef guide, but only briefly. It makes all the difference in the world, especially when you start messing with others cookbooks. 

Adding them to vagrant is as easy as adding the following to your Vagrantfile:

<code>
chef.cookbooks_path = ["chef-repo/cookbooks", "chef-repo/site-cookbooks"]
</code> 

### Managing packages

For some reason, the 'package' command in chef doesn't always work well in vagrant. Chef runs under the vagrant user, rather than root, so anything that requires super-user powers, needs to be handcoded. So to install emacs, you can't just do:
<code><pre>
package "emacs" do
	action :install
end</pre></code>

But instead have to do (assuing your are on a debian system, adjust for you own package manager):
<code><pre>
bash "install emacs" do
  user "root"
  code &lt&lt-EOH
  apt-get update
  apt-get install -y -q emacs
  EOH
end</pre></code>

Here, we are essentially just running a shell command, as root, that (1) updates the apt-get repository and then (2) will install emacs.



[CFEngine]:
[Puppet]:
[Umbria]:
[chef cookbooks]:
[this tutorial]:
[official wiki]:
[terms guide]:
[Vagrant tutorial]:
[blog post]:
