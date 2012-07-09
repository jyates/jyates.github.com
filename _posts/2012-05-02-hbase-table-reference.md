---
layout: post
title: Table References in the HBase Shell
tags: hbase, reference, ruby, jruby, big data
description: How to use the new table references in the HBase shell
location: San Francisco, CA
---
As of HBase v0.96 (currently trunk), one can now get a reference to a table in the client shell. This is huge news for the hbase shell - the biggest update since the security features were added.

The HBase shell is actually a specialized jruby REPL, preloaded with a bunch of specialized HBase functionality. One of the things that always bothered me about the shell was that evn though Ruby is object-oriented AND HTables are objects, _you couldn't get a reference to an HTable_, you had to use the top-level put, get, scan, etc. methods and specify the table name _each time_. A typical test that the shell is working, might look something like this:

{% highlight bash %}
hbase> create 't1', {NAME => 'f1', VERSIONS => 5}
hbase> put 't1', 'x', 'f1', 'v'
hbase> scan 't1'
hbase> disable 't1'
hbase> drop 't1'
{% endhighlight %}

In this little test, you are doing the following:
1. create a table named 't1' with the column family 'f1' and keeping 5 versions
2. putting a single row 'x' into table 't1'
3. scaning all the rows in 't1'
4. disable 't1'
5. dropping 't1'

Anything in there seem really redundant? Accumulo solves this by have a 'table context' in their shell, but I've always found it a little odd and easy to forget which which commands apply in which context (general or context or both?). 

Instead, in HBase we use a table reference which lets you use all the commands without having to worry about context and at the same time simpliying manipulating tables. To do the same test as before,  but with a lot less effort (especially with longer named tables) you can do the following:

{% highlight bash %}
hbase> t =  create 't1', {NAME => 'f1', VERSIONS => 5}
hbase> t.put 'x', 'f1', 'v'
hbase> t.scan
hbase> t.disable
hbase> t.drop{% endhighlight %}

What is really neat is the addition of the 'get a table' functionality. If you have already created a table, say named 't1', the above example could look something like:

{% highlight bash %}hbase> t =  get_table 't1'
hbase> t.put 'x', 'f1', 'v'
...{% endhighlight %}

Any of the more complex invocations of the table methods (get, put, etc) also work on the table reference - just like you would expect!

 To get more information on how to use that command, you can use either:

{% highlight bash %}hbase> help 'put'{% endhighlight %}

OR if you have a reference to a table,

{% highlight bash %}hbase> t.help 'put'{% endhighlight %}

Similarly, to get general help for a table, you can:
{% highlight bash %}hbase> table_help{% endhighlight %}

OR if you have a reference to a table,

{% highlight bash %}hbase> t.help {% endhighlight %}

Note that table references also will also you to tab-complete the manipulations on the table reference. One of the great advantages of using the ruby REPL and a real Table object. 

## High Level Implementation Details
Internally, a Table has a bunch of 'internal methods' that do the actual low level calls to the HTable reference (with a little massaging). This allows the user to much more easily tab-complete and find the correct methods first, e.g. get, put, scan, describe, etc., and the internal methods are accessed via calls to <pre>_name_internal</pre> which in practice are things like: <pre>_get_internal</pre> or <pre>_delete_internal</pre>

Each of the top level commands binds its named command to the Table at load time, allowing it to wrap both call paths - from the shell and from a table reference - with a formatter and do all the 'nice' things we have come to expect from things in the HBase shell. 

The admin commands on a table are a little bit different than the table commands. Since the admin each time will create a new HTable to modify the table we only need to ensure that we pass in the name of the current table. Implementation wise, this means we have a static call back to the shell that takes the name of the current table and binds at load time to a bunch of list of strings in the table. This allows us to keep track of which admin commands we are binding to a table, but keeps all the implementation details out of the table class (in the same way that the HBaseAdmin doesn't have code in the HTable, except for all the late binding sugar).

All of these changes were enabled by the highly dynamic nature of Ruby. You can dynamically bind methods at run-time to classes - the ability to reopen a class and modify it. Further, since everything in Ruby is a message, we can also bind via strings in the method table for a class. Pretty cool stuff!

If you are interested in taking a look at how we do this in HBase you can look the [shell.rb] (https://github.com/apache/hbase/blob/trunk/src/main/ruby/shell.rb) and the class methods in [table.rb] (https://github.com/apache/hbase/blob/trunk/src/main/ruby/hbase/table.rb).

## Caveats 
Currently a table reference does *not* support many of the admin commands on a table, things like 'truncate' or 'alter'. This is still a pretty new feature, so as people require/want this functionality, they can add it (yay open source!); recently [HBASE-5921] (https://issues.apache.org/jira/browse/HBASE-5921) was filed around this issue. Its a pretty easy fix and hopefully I'll get around to it next week. Further, all the per-table security features are not yet supported either - its likely as the security code becomes more widely used this will be the case, but that remains to be seen. 
