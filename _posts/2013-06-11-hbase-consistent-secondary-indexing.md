---
layout: post
title: HBase Consistent Secondary Indexing
location: San Francisco, CA
subtitle:
description: Secondary indexing for HBase is a difficult problem, but remains perenially popular. Various implementations exist, but all fall short either in features or latency. [Phoenix](http://www.github.com/forcedotcom/phoenix) is soon gaining support for what we consider "HBase Consistent" Secondary indexing.
tags: hbase. phoenix, secondary indexing, si, big data
---

When using a database you don't always want to read the data in the same way that you wrote it. For instance, if you have a database of items in your store, you might insert them by name, but some days might want to query the database for all the items that came in on Monday.

The naive case would have to scan through all the items in the database to find just those that came in Monday - clearly not a very scalable solution! Secondary indexes allow you a 'secondary' mechanism by which to read the database; they store the data in an index which is optimized to be read for an othogonal facet of your data (for instance, date of arrival). 

Wikipedia defines secondary indexing as:
" --- WIKI quote --"

Traditional, single (or a small cluster) server databases achieve secondary indexes by updating a 'index table' which stores the store in the query-organized layout in a transaction with the update to the primary table. This works fine because there is very little overhead - no need to go across the network or rely on complex coordination. Everything is nicely ACID and works with the existing model.

HBase doesn't play as nice.

## HBase Problems

HBase is built to scale by sharding the data between different 'regions' that could live anywhere on the cluster. Each region is (almost) entirely independent from every other region in the cluster - this allows us to scale up the number of regions as our data size grows and not worry about performance.

The problem with secondary indexing then is that we are then attempting to add this cross-region interaction on a system whose very basis is *to not have cross-region dependencies*! At first blush, this is the very definition of an impedance mismatch.

## Old News - An Overview of Existing Options

People have tried many times in the past to implement secondary indexing over HBase - things like [Lily] and [HBaseSI] attempt to tackle the problem head on. 

Lily builds its own Write-Ahead Log (WAL) framework on HBase - this gives us most of the expected semantics but at a rather high latency cost. For some use cases, this is fine - if this is you, you can stop reading and go call up the Lily folks.

HBaseSI is an alternative approach but doesn't work well with Scans - its designed for point Gets. However, the general use-case for HBase is multi-row scans, this doesn't translate into a general solution.

People have also attempted to do full transactions on HBase, things like [Omid] and [Percolator] - once you have full transactions between tables, adding secondary indexes are trivial, they are just another transaction. The downfall here, as expected, is the overhead. In a distributed system, you end up creating massive bottlenecks that dramatically reduce the throughput (and increase the latency) of the entire system. For most people, this has proven too much overhead.

Then people have attempted to do secondary indexing through the application. While this could very well work, it is rarely going to be generally applicable and further, is going to be very brittle. Secondary indexing is properly a function of the database and should be closely tied to its internals to support efficient and correct implementations.

Recently, a some work has come up to provide in-region indexing. Essentially, we provide a secondary index on a given Region. Then when querying along the index, we need to talk to each reqion's index to determine if that region contains the row. The obvious downside is a dramatic effects on throughput on latency. Where previously we only had to talk to one server, suddenly we have to talk to *all* the servers and cannot continue until we get a response back from all of them (otherwise, we might miss a positive response). If you are willing to take this latency hit, it can be an acceptable solution - its fully ACID within the HBase semantics.

There has already been a lot of published thought on other ways we could do secondary indexing - I took a crack [here] and Lars Hofhansl has written some thoughts [here] and [here]. However, all these proposals are either (1) wrong in small corner cases or (2) inefficient. We can do better...

## Redefine the problem

What if we don't need to support full ACID semantics? I mean, HBase doesn't provide them, so why should our indexing solution need to provide _stronger guarantees than HBase_?

Hmmm, okay, maybe this could take us somewhere…. Let's look at what we _do_ need to provide.

### Durability

First, we certainly need to proivde *durability* (aciD). 

Lily does this by providing its own WAL implementation. A simpler version (rememeber, we don't need ACI of ACID) would write to a WAL table then then replay the WAL when doing updates. However, this ends up requiring at least a 4x write of the data (once to the WAL table, which writes to an HBASe WAL, and then to each of the involved tables). And keep in mind that you also need to read the WAL each time you are doing a read and then merge those changes back into the Results on the client. This is going to get rough really fast. 

Well, wait a second - HBase already has a WAL! Maybe we can tap into that…

By tacking on custom KeyValues (lets call them, oh, I don't know, IndexedKeyValues) to the WALEdit we can serialize our index updates to the WAL using the usual Writable#write method. In fact, this means we don't even need to have a backing byte array in our IndexedKeyValue! In HBase 0.96/0.98 its a little bit different, but conceptually the same.

The only problem then is making sure that we can read these edits back again. In HBase 0.94.9 (the next release), we can provide a custom WALEditCodec which manages the reading/writing of KeyValues in the WALEdit to/from the WAL - this is by far the cleaner mechanism and exactly how we would support indexing on 0.96/0.98 (we don't yet, but it's a minor port). In <0.94.9, we need to provide a custom HLogReader - an IndexedHLogReader - that can figure out the type of the serialized KeyValue, either an IndexedKeyValue or a regular KeyValue. 

Great! Now we have durability of our index update AND a way to read it back.

### Getting back to ACID

Now, what kind of guarantees can we provide? So far we only have the "D" in "ACID". We were able to make some big strides by thinking about how we can leverage HBase, lets see if we can do that again.

Previously, we always expected the client to define all the index updates to make at write time. It was always smart enough to break out the update into the required updates to the index table and then just send all of those to the database. The database here just needs to provide the base intelligence to apply the updates.

What if, instead, we push down the work to the server? It would be the same ampunt of data transfer. Originally, it was once to the pimary table and then once to each index table. If we push down to the server its a primay update to the primary region and then from there out to each of the index tables. There is a bit of an throughput concern here (we have to serialize the process a bit, rather than making the updates in parallel), but its relatively minimal… and we'll talk about how we could alleviate this later.

Since the region - or rather a RegionObserver Coprocessor - builds and writes the index update it should be able to manage the _consistency_ (aCiD) of the updates. Remember that HBase doesn't make any serializability guarantees between clients (see [my previous blog post] abount managing this with external time) - all we need to guarantee is that the index updates eventually make it.

Therefore, lets tie the primary and the index updates together. When we get a write, the coprocessor builds up IndexedKeyValues that contain the index update information and we attach them to the WALEdit for the primary table Mutation. Once this gets written to the WAL its expected to be durable -  we can then attempt to send the index updates to the index tables. 

#### Facing Failure

If any of the index updates fails, we need to ensure that it gets reattempted. The simplest way to do this is to kill the server, which will trigger the standard WAL replay mechansims. By hooking into this replay mechanism, we can pull out our index updates and replay them to the index table, which has hopefully recovered by this time. Killing the server has a dual benefit of being hard to miss - if the index table is incorrectly configured (i.e. it doesn't exist), your cluster will quickly shut down, altering you to the problem. This gets us _atomicity_  and _isolation_ in the [HBase world](http://hbase.apache.org/acid-semantics.html) - updates to the index will always occur, but are not guaranteed to be performed at exactly the same time or order with other updates.

That is a bit of an extreme failure scenario, but follows a 'fail fast, fail hard' paradigm - not always robust, but ensures correctness. There are other, potential mechanisms to handle missed index updates, for instance, marking an index as invalid and rebuilding later. However, this is a bit more complex to handle and outside the scope of this 'bare bones' indexing solution.

### HBase ACID

If you are using HBase, there are some things you give up - cross-row guarantees. However, once you can see the data, its durable. By leveraging the WAL replay mechansims in concert with careful management of the WAL (ensureing the correct edits get replayed) we get the same ACID guarantees with our index updates that HBase makes of our primary row updates. 

See the [HBase Reference Guide](http://hbase.apache.org/acid-semantics.html) if you want a more indepth treatment of the what ACID guarantees HBase makes.

## Not just fluff

The above discussion is not just a theoretical investigation on how one _might_ implement secondary indexing - this is actually what we have done. Initially, [hbase-index] is being released as a subproject under Phoenix, but there are discussions around moving this into the HBase core. 

hbase-index is designed to be a transparent layer between the client and the rest of HBase - nothing is tying it to Phoenx and can be used entirely indepently. However, Phoenix support for hbase-index is currently in progress at Salesforce (see the [github issue](https://github.com/forcedotcom/phoenix/issues/4) where James lays out the internals) and will be completely transparent to Phoenix clients. If you don't want to use Phoenix, you can easily create your own IndexBuilder to create the index updates that need to be made. 


### Constraints

There are several constraints of the *current implementation*. None are insurmountable, but merely artifacts of a new project.

First, we only support Put and Delete mutations. This is because they provide sufficent hooks into the WAL for a RegionObserver. There is no theoretical reason we can't support other types for HBase, but rather the practical matter of getting the support into HBase.

That brings us into the realm of HBase versioning - we only support WAL Compression on HBase >= 0.94.9 (soon to be released). As of 0.94.9, we can plug in our custom WALEditCodec which manages the compression logic. We don't support HBase 0.94.[5-8] as there are several minor bugs that prevent the hbase-index from functioning. Indexing is supported in HBase 0.94.[0…4], but without WAL Compression. Currently, only the 0.94 series is supported as we are initially targeting Phoenix adoption, but moving to 0.96/0.98 should be a trivial matter.

Right now, we don't support the built-in HBase replication. The major problem here is that the replication mechanims are not pluggable, making it impossible to use the same custom KeyValue mechansims that we previously employed. This is not an inusrmountable change (and plays well with much of the current work on the HBase internals already being done in the community), but merely one that takes time.

Also, as mentioned above, we end up putting a lot of load on each Region - it has to build up all the index updates and write them to the other tables, all while doing all its usual work. This is could slow us down a little bit. Alternatively, we could use the same WALEdit/IndexedKeyValue mechanism but just provide a locking mechanism on the WAL. The client is required to make the index updates after making the indexed Mutation to the primary table - all the server does is ensure that we don't roll the WAL until the index updates have been made. While this sounds great, it introduces a lot more complexity around when to trigger failures and mangaing client writes concurrently with the WAL.

## Conclusion

There have been a lot of discussion and work around secondary indexes over the last few years. Everyone wants it, but no one is willing to give up certain things (speed, traditional ACID, HBase features) to get secondary indexing. We aren't proposing that this solution is a one-size fits all; if you need full consistency between indexes and the primary table, then this won't be enough. However, if you are already using HBase and willing to continue those semantics, hbase-index provides an easy framework to build your own indicies.

By leveraging a RegionObserver that creates custom KeyValues we can be sure all updates are stored into the WAL, giving us the expected durability. This coprocessor then also makes the index updates and fails the server if we cannot make them, triggering a WAL replay and another attempt to update the index. While a bit drastic, these 'fail hard' semantics make it difficult to avoid seeing an error - quickly alerting when your index table is misconfigured.

This isn't a vaporware, the code is already out there [on github] and support is coming to Phoenix. Think this stuff is cool? Then we would love to have you comment on the project or even write some code!