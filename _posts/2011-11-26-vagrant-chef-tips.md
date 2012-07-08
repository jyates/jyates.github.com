---
layout: post
title: Vagrant + Chef - Tips and Tricks
---

# Vagrant + Chef - Tips and Tricks

November 26, 2011 - Somewhere over the continental US

There is a good chance you got here looking for a solution to some fincky problems with vagrant and/or chef. Congrats on using some cool tools! If not, I'm going to introduce the tech I'm talking about and why you should even care. So if 'get it' already, then skip on down to the <a href="#interesting">interesting features</a> discussion - some caveats and tricks might need to keep in mind when working with vagrant + chef.

## Chef 

Imagine that you are expanding your Hadoop cluster and want to add another data node. Well, that means you are going to have to install java, download the correct hadoop version, update the configuration files, and startup the datanode process. That can be a bit of a pain for your sys-admins, though with practice you can probably get it down to 5-10 minutes of scping and fiddling. What if you could do it with a push of a button and you _know its exactly the same_ as every other datanode in your cluster?  

Or what if you need to replace your Job Tracker? Push a button. 

Or your web server goes down? Push a button.

If you haven't heard of it by now, Chef is one of the easiest ways (feel free to rant about the qualities of [CFEngine] or [Puppet] in the comments) to manage the configuration of your computers, particularly in a cluster (though there are certain places where they really aren't 'cluster aware', see [Ambari]). There a a bunch of tools out there to manage this, but they all follow the same basic idea.

You store the configuration of each 'role' on some remote server. Then when you fire up a new machine, you point it at that server it downloads the configuration it 'should' look like, and then tries to build itself up to that configuration. If everything goes as planned, then your machine looks like the configuration you specified. Every time. What if you want to update that configuration? Just do it in one central location and then push it out the necessary recievers. All of a sudden, instead of doing tons of parallel-sshing and manually setting things up (or home rolling your own scripts) you can do it all in this one tool. 

Yes, this is pain learning the system. And yes there is pain in writing the recipes. However, do it _once_ and then you have it, potentially, for years. 

Its worth it - trust me. If you don't, then do the math - still worth the time to learn the system and write your node recipes.

Chef is nice because it is all in Ruby (and a special, Ruby-like DSL). This means anything you can do in Ruby, you can do in Chef. This means is is really easy and natural to do more complex configurations. There is also a pretty strong (and growing) community around Chef, with tons of people open sourcing their own Cookbooks. The short story of that is, you can get up and running in minutes AND you have great examples of how to write your own cookbooks.

If you want to learn more about Chef, I would recommened the [official wiki/tutorial] - which is pretty dang good - and this [terms guide] to help keep your head on straight).To people not used to these systems, it can be a lot to wrap your head around (it was at first for me too), but once you understand the paradigm, its simple to write your own recipes and leverage the others out there. On top of that there are a variety of [chef cookbooks] out there. They will help you get started as well as seeing 

There are also a couple of ways you can try out chef. First, you could run your own chef server. Its a little daunting, to jump to that immediately (though the offical wiki has some great info on how to do that). Next easiest step is Opscode's option to try out 'Hosted Chef', where they run the configuration sever for you and (even better) its free to try on up to 5 nodes (aweome!). The last thing you can also try is to use Vagrant. Vagrant is a tool to dynamically build virtual machines; the dynamic part comes from using Chef to configure the VM. No remote server and minimal configuration pain (though there is some 'fun' associated with it). 

So there really isn't any reason not to try chef, if you haven't already. 

## Vagrant

Ever wanted to ...
 * try out some new software but don't want to mess with your home machine
 * setup a standard developer environment for new developers
 * automatically build a virtual machine from code (without wanting to shoot yourself)

Then Vagrant is for you! Vagrant will let you do all of these things and more. Essentially, with the push of a button you create and then configure the virtual machine. Another one-line command tears it down (actually it's just <code>vagrant destroy</code>). What's really great is that Vagrant can leverage either Chef _OR_ Puppet (two of the most widely used configuration management systems) so many people will be able to leverage a bunch of your current skills. Vagrant also provides a safe and (relatively) easy way to learn Chef or Puppet.

Personally, I wanted to learn Chef and a project using Vagrant (dynamically building a VM for doing Accumulo training) offered the perfect opportunity. Plus, I'm also in the process of learning Ruby (following some of the recommendations in [Pragmatic Programmer] (http://www.amazon.com/Pragmatic-Programmer-Journeyman-Master/dp/020161622X). Try it out, it pays off in the end, and well before it too!), so a real world application was a great way of getting my hands dirty. Vagrant is also heavily Ruby based, so IMHO another point in its favor. In fact all the configuration is actually done in Ruby. Also, most of the documentation out there is for chef, so thats an easy call. Hmmm, interesting how the amount of documentation influences tool choice, isn't it? I'll get back to this later.

Everything in Vagrant is based on around the Vagrantfile. Think of this as your pom.xml (if you like maven) or build.xml (for those ant people) or make script (remember those?). The default vagrant file is chock full of documentation around which options you can select and which you would need depending on using Chef or Puppet. The official [Vagrant tutorial] and [getting started] guides are actually pretty fantastic. It's definitely your first stop in getting the system up and running. Go ahead, try it. Work through the examples.  

...(type type type)...

Buuuuut, the tutorial and docs don't cover all the intracies everything you say? Fair enough, your problems (I'm guessing) are probably stemming from chef. Check out this great [blog post] about how to use chef+vagrant together.

At this point, I figure you have a pretty good handle on vagrant. Lots of Ruby goodness and some pretty good docs for things that aren't totally apparent. If the virtual machine is actually going to be pushed out to a server and used as a vm, you can forget about turning on gui support. However, if you are just playing around, its rather nice to have. Just remember the defaults:
<code><pre>username: vagrant
password: vagrant</pre></code>
as the gui doesn't automatically log you in like the usual ssh connection will take care of for you. 

Also, if you are already running Chef to do configuration, Vagrant is really nice in that gives you the option of configuration form the Chef-Server, rather than from local files. This is more like a production(ish) situation, so its another, gentler way to work up to using chef in a 'real world' system.


# <a name="interesting">Tips and Tricks</a> (pain points solved!)

There are some facets of the interaction of vagrant and chef that can cause problems. Hopefully, the resources above answered your questions, but if not, lets dig into some of the things I found.

### Read what's out there

Seriously, go do it. A lot of the recipes are really nice (the java one in the standard community is great) and will teach you a _ton_ about how to write good recipes. At the same time, there are a bunch of recipes out there that are crap - don't worry, you'll learn a lot from those too (about what not to do). That's part of the beauty of open source, and you would be foolish to not take advantage of it.

Also, think about doing [readme driven development]. It works really nicely with chef recipes which end up being very modular and easy to work with in the readme style. And at the end, you've already done all your documentation! No need to try and remember all the possible knobs and options, no double checking to make sure you have the right calls - you did it when you wrote it, so its pretty close to perfect.

### Setting up your directories (and version control _everything_)

Keep a separate site-cookbooks for your own cookbooks. Its mentioned in the chef guide, but only briefly. It makes all the difference in the world, especially when you start messing with others cookbooks. 

Adding them to vagrant is as easy as adding the following to your Vagrantfile:

{% highlight bash %}
chef.cookbooks_path = ["chef-repo/cookbooks", "chef-repo/site-cookbooks"]
{% endhighlight %}


All the regular (external) cookbooks that you pulled down from various open source repositories should go into chef-repo/cookbooks. Then everything that you write should go into chef-repo/site-cookbookbooks. That way you know which things came from where and who tell email if things start breaking.

Its debatable whether you want one master git repository (say in chef-repo) and then add submodules for each cookbook or creating a new git repo for each cookbook. Personally, I like to go with the latter since it ends up being _much_ cleaner than dealing with submodules in git (for those interested, you can read about submodules - and the 'fun' associated with them - [here] (http://progit.org/book/ch6-6.html)

### Chef Solo

The whole way vagrant works is to run a chef-solo instance using files copied into its /tmp directory. Specifcally, you will have a directory under /tmp for each of the cookbooks folders (copies of those folders in fact). This is where everything is run from and where you will need to check to grep the logs and see what happened (though vagrant has some pretty good info when it fails already).

That being said, it can ofte be incredibly convenient to leverage vagrant's data copying over using the data bags available in Chef. It just ends up being less to write using Vagrant. However, this will impact all your recipes and is not recommended unless you are _100%_ sure that you will need to copy those files over every time. Otherwise, put it into a recipe; it will take you about 5 extra minutes, but could save you 10x as much in debugging later on.

Do the right thing.

### Managing packages

For some reason, the 'package' command in chef doesn't always work well in vagrant. Chef runs under the vagrant user, rather than root, so anything that requires super-user powers, needs to be handcoded. So to install emacs, you can't just do:
{% highlight bash %}
package "emacs" do
	action :install
end
{% endhighlight  %}

But instead have to do (assuing your are on a debian system, adjust for you own package manager):
{% highlight bash %}
bash "install emacs" do
  user "root"
  code <<-EOH
  apt-get update
  apt-get install -y -q emacs
  EOH
end
{% endhighlight %}

Here, we are essentially just running a shell command, as root, that (1) updates the apt-get repository and then (2) will install emacs.

### One top level recipe

If you are building a dev machine or a handful of roles, it is _much_ easier to just make a single recipe. Yes, Chef provides the idea of roles...but do you really need to have a dev machine that is a Datanode and gerrit server? Probably not. So just make a recipe for each. If you really need to add things together, then you can use a can pull in each recipe as needed.

### Veewee

Veewee is the easiest way out there to build your own Vagrant 'base box' from scratch. It will take care of a lot of the hard work for you, if you aren't happy with the standard, available base boxes out there.

It's currently on github [here] (https://github.com/jedi4ever/veewee). Its definitely worth looking into if you are doing serious customizations.

Those are all the tips and tricks I have for you today boys and girls. Hopefully you found this helpful and are going to go out devops your wildest dreams.

[readme driven development]: http://tom.preston-werner.com/2010/08/23/readme-driven-development.html
[CFEngine]: http://cfengine.com/
[Puppet]: http://puppetlabs.com/
[Ambari]: http://incubator.apache.org/projects/ambari.html
[chef cookbooks]: http://community.opscode.com/cookbooks/
[official wiki/tutorial]: http://wiki.opscode.com/display/chef/About
[terms guide]: http://kallistec.com/2010/02/01/the-chef-way-episode-2-chef-speak/
[getting started]: http://vagrantup.com/docs/getting-started/index.html
[Vagrant tutorial]: http://vagrantup.com/docs/index.html
[blog post]: http://www.jedi.be/blog/2011/03/28/using-vagrant-as-a-team/
