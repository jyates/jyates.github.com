---
layout: post
title: From Git Noob to Wizard in 5 minutes
location: San Francisco, CA
subtitle:
tags: git, shortcuts, efficiency
---

Looking to improve your efficiency with Git? Learn the secrets to go from novice to master to wizard. Not only that, but it can make life significantly easier and faster - every day.

<img src="/images/posts/git/git_flow.png"/>
[Source](https://hackernoon.com/top-5-free-courses-to-learn-git-and-github-best-of-lot-2f394c6533b0)

# Basics

Simple git aliasing is the easy way to get started with short cuts. They even integrate into the git auto-completions + suggestions, so if you misspell a shortcut it will likely recommend the right thing!

Here's some things that I have in my `~/.gitconfig`

```
[alias]
  b = branch
  patch = apply
  spatch = apply --summary
  st = status
  # Fix the current commit, adding any changes for 'tracked' files
  amit = commit -a -m
  amend = commit -a --amend

  # Rebase help
  ##############
  abort = rebase --abort
  continue = rebase --continue
  skip = rebase --skip
  cp = cherry-pick

  # commands to list commits
  ##########################
  # simple log printing
  glog = log --pretty
  # simple list
  ls = log --pretty=format:"%C(yellow)%h%Cred%d\\ %Creset%s%Cblue\\ [%cn]" --decorate
  # exact dates
  ll = log --pretty=format:"%C(yellow)%h\\ %ad%Cred%d\\ %Creset%s%Cblue\\ [%cn]" --decorate --date=short

  # branch manipulation
  ####################
  trunk = checkout trunk
  master = checkout master
```

These are all things you might find in your standard set of suggested shortcuts anywhere around the interwebs.

# Shelling out

Once you are starting to get used to shortcuts in git you will likely run into things than are more complicated that just a single command. This is where shelling out becomes useful. You can alias a git command to a series of shell commands.

Often, I just chain git commands together, to save my self the typing, i.e. for common workflows.

```
[alias]
...
  # aggressively cleanup any files or changes
  purge = "!sh -c 'git clean -f; git checkout -- .' -"

  # If you forget this is git, and not maven
  #####################################
  generate-sources = !mvn clean generate-sources
  test = !mvn clean test
  # checkout a branch and then re-generate mvn sources. Created before golang was a thing :-/
  go = "!sh -c 'git checkout $1 && mvn clean generate-sources' -"

  #redirect gitk stderr to /dev/null b/c it is dumping lines like: 2012-08-02 21:14:49.246 Wish[33464:707] CFURLCopyResourcePropertyForKey failed because it was passed this URL which has no scheme:
  k = !gitk --all 2>/dev/null
```

A small aside on that last alias - `k`. It helps with logging from [gitk](https://lostechies.com/joshuaflanagan/2010/09/03/use-gitk-to-understand-git/), 
a simple UI that I findly very helpful when trying to visualize all the branches and their locations. Some prefer to 
do this with fancier command-line verions of `git log`, but for my money its hard to beat the simple navigability of 
gitk.

# Branch in the command prompt

This is a super useful, easy addition to your command prompt that dramatically improves your life, especially if you 
have multiple git repos. It takes your command prompt from

```bash
jyates@home$ 
```

to

```bash
jyates@home (git-wizardry)$ 
```

Its pretty simple to add too. At the end of your `~/.bashrc` you can 
just include:

```bash
# Print out the current branch name, if we are in a git repo. Takes the last
# error code as a parameter, and then returns that same error code, so that you
# can continue to have a correct $? output
function parse_git_branch () {
  git branch 2> /dev/null | sed -e '/^[^*]/d' -e 's/* \(.*\)/ (\1)/'
  return $1
}

PS1=${PS1}$(parse_git_branch $?)
```

If you are in a git repo, it shows which branch your are on. If you aren't, it doesn't show anything. Pretty neat.

# Getting fancy

For years I've been wanting to switch between branches like directories and trim branches that get merged. Without 
further ado, here are the additions to your ~/.gitconfig

```
[alias]
  # not only switch branches, but store the branch I was on
  co = "!git rev-parse --abbrev-ref HEAD > ~/.git_current_branch/${PWD##*/} && git checkout"
  # go to the last stored branch
  cd = "! sh -c 'cat ~/.git_current_branch/${PWD##*/} | xargs git co'"
  # delete the last stored branch
  dlast = "!git b -d $(cat ~/.git_current_branch/${PWD##*/})"
```

Don't forget to create the `~/.git_current_branch` directory, otherwise these commands will break.


Ok, so ... what does that all mean and why should you care? This set of aliases often come up when I am switching 
between branches and working on features. For instance, my workflow - similar to the standard git branching model - 
is something like:

```bash
(master) $ git co working-branch
... write code
(working-branch) $ git amit "A super cool feature"; git push origin
... code review, merged code
(working-branch) $ git cd
Switched to branch 'master'
Your branch is up-to-date with 'origin/master'.
(master) $ git pull origin
... pulling changes
(master) $ git dlast
Deleted branch working-branch (was 31de864).
```

A simple flow, but something I do multiple times a day and helps keep my workspace nice and tidy.


# Shelling out with auto-completion

Shelling out in git commands means that git can't easily figure out which alias command should be recommended. 
Fortunately, git has hooks for bash functions (at least in newer versions) to find the root. Basically, it takes the
command you enter and apply its recommendation function using functions that start with `_git_`.

Maybe this is easier with an example. Let's ensure that our custom `co` function autocompletes like the normal `checkout` function:

```
# Wrapper git functions for auto completion
###########################################
function _git_co() {
  _git_checkout
}
```

As long as these functions are sourced, ideally as part of your ~/.bashrc, then they get picked up and correctly auto-completed.

# Other helpful commands

I also like to keep track of my progress and things to do in my git history. To that end, I like to have these 
functions in my `.bashrc`

```
# Git Functions
###############
#add todo for git
todo(){
  git commit --allow-empty -m "TODO: $*"
}

#add epic tood for git
epic(){
  git commit --allow-empty -m "[EPIC]: $*"
}
```

Hopefully you found some of these commands useful and will help you same time and effort every single day!
