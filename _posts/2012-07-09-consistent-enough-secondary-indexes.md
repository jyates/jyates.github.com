---
layout: post
title: Consistent Enough Secondary Indexing
location: San Francisco, CA
description: Distributed systems inherently trade-off consistency or availability, making it very difficult to get a consistent secondary indexing system. Let's talk about ow to actually make this happen
tags: hbase, bigtable, second, secondary index, consistency, availiabilty, CAP Theorem
subtitle: Subtitle
published: false
---
Secondary indexing is the solution to finding information quickly that is not the 'primary key' of the entity you are looking up. In an RDBMS, each entity is orgcanize based on primary key - either a random GUID or a key identifying piece of data that is unique (e.g. for a person, social security number). However, that entity has other informatin associated with it that you might want to look up. Going back to our person example, you might want to find all the people living at '123 Jump Street'. With the current configuration, you would have to scan the _entire person table_, looking at each record to see if that person lives at '123 Jump Street' - potentially huge, time consuming query. 

The idea behind secondary indexes is that we 'index' the address field of all the people in our database into another table. The primary key of the secondary index table is just addresses and then it stores all the primary keys (social security numbers) of people living at that address. We are trading space for speed and in very large queries, this tradeoff is entirely acceptable.

To  find all the people living at '123 Jump Street' you can the just jump right to that row, and get the all the  where the primary key is the address. Then to find all the people living at '123 Jump Street' you can the just jump right to that row, and get the all the primary keys of the people living there and look them up directly in the 'people' table. This is vastly more time efficient that doing a scan of the entire people table, looking at each record to see if they live at '123 Jump Street'. 

Creating secondary indexes in an RDBMS  is a very natural fit - its just a transaction where you are doing an update to the index everytime you do an update to a row that is indexed. For example, if a person moves, we update that information in the 'people' table and at the same time update address index table. Since this is a full transaction, if the client or server fails halfway through the transaction, we will never have an inconsistent state, so reading the location row always gives us the latest information.

The problem with this approach for a distributed system is that transactions are _hard_. To be completely safe they require a Paxos-like protocol to complete, which can be very costly timewise. You can play some tricks with optimistic locking to get good overall performance, but its still very hard to ensure that the 99th percentil times are good.  

In the past I've worked on some code to do secondary indexing ([Culvert](http://github.com/jyates/culvert)) for a BigTable like system, but the indexing was of secondary importatnce (no pun intended), to the rest of the work of doing SQL over BigTable like systems. Culvert is almost there in terms of overall correctness, but trades-off full consistency for speed and ease. We decided that a dirty index (false positives) and fast writes were more important than having a fully transactional/consistent index. 

There are ways to do the latter, and the guys over at [Lily] (http://www.lilyproject.org/lily/about/playground/hbaseindexes.html) have done a terrific amount of work to make it possible. However, Lily has a lot of moving parts (secondary servers, a full write ahead log, etc.) that make it inherently hard to use and slow.  Don't get me wrong - they are great guys and are doing great work, but we can do better.

There are a couple of pieces that we need to put together to enable consistent secondary indexes. The first is the realization that we don't need to be _transactional_ - indexing can be an idempotent operation if we bind the writes to a timestamp (the second part).

Let that sink in for a moment... it means we can essentially 'cheat' indexing and still always be right (or right enough). Its been knownt that secondary indexing in a distributed environment is inherently easier that full transactions, but its rarely articulated why its easier. Idempotence allows us to retry without concern about currentl running operations or worry about previous effects. This is huge. A game changer.

Architectually, the first piece we would like to have is a globally unique id generator, allowing each client to apply the same write number to each batch of writes. Something like this is discussed in [Percolator] (hhtp://research.google.com/pubs/pub36726.html), and I've recently built something similar ontop of HBase that will be open sourced in the near future (check back on my [github] (http://github.com/jytates) page).

We then have two  major concerns - making sure we never get a 'wrong' answer from the index (even if the client or server crashes) and making an indexed write _fast_. If we didn't care about the latter, we could do two-phase commit with a WAL and get correctness, but its going to be really slow due to multiple writes of data and potentially highly contentious with respect to getting the right locks (for multi-row writes). 

Instead, we split the effort by making the client a little bit smarter and adding some more manipulation on the server-side. 

## (Hand-waving) proof by example

Let's walk through the implementation via an example. 

Going back to the people example, say we want to add a new person to our database where we are indexing on address (just a simple inverted index example, but you could do smarter things like pre-joins, etc in the index with the nearly exactly same methodology).

The client then has to do a couple of things. First, it gets a write number from our global generator to apply to this batch of writes - this allows us to reason between the index state and the primary table state cleanly, but is still possible if you set write timestamps from the client. 

Once it has a write number, the client _first_ writes to the _address index table_ - the address row gets updated with the person's SSN. It looks something like:

{% highlight bash %}
    Address     |      SSN     |  timestamp
123 Jump Street |  111-22-3333 |      2
                |  111-22-3333 |      2
                |  333-44-5555 |     14
{% endhighlight %}

supposing that the someone with SSN=111-22-3333 already lives at 123 Jump Street and we added person SSN=333-44-5555. After successfully writing to the index table, the client then write the same person to the primary table:

{% highlight bash %}
   SSN     |     Address     | timestamp
333-44-555 | 123 Jump Street |    14
{% endhighlight %}

Then when another client attempts to lookup who lives at 123 Jump Street and will see two SSNs, the one that was already there and the one that we added. Secondary index built and working.

## Failure Situations
While this indexing scheme works when no component fail, we still need to look at how we deal with failure situations, which are very likely on the commodity hardware that runs the largest clusters in the world. 

Suppose the client fails before either write hits the wire - no problems, we still have a consistent system (okay, that was a gimme). Same story for if the client if both the writes succeed - another easy one.

Note that we can never get into a situation where there is a write to the client table, but not to the index table becuase we always guarantee that the index table succeeds before writing to the primary table. You can do this by having the client write to the index table, waiting for success and then writing the primary table or via an indexing coprocessor that writes the indexes to all the tables before it writes to the primary table - these are just implementation details (for the record the latter would be faster and clearn from a client perspective, but a bit more difficult to implement). Either way, you will _never have false negatives_ in the index table, you will only get false positives in the worst - partial failure - case.

False positives occur when the first write - to the index table - succeeds, but the write to the primary table fails. This puts us in a little bit of an odd situation because the index table says a row should exist, but the primary table doesn't have that edit. We know the edit belongs to that index because of two key points: 

 * both writes have the same timestamp  
 * each timestamp is unique (via the timestamp generator), so it must have come from a partially-successful write

allowing us to ignore the failed edit. Basically any row in the primary table with a key who's latest update doesn't match the expected timestamp (allowing us to keep multiple versions of the table back), can be considered a broken link and lazily cleaned up. Note that we don't actually need to use the latest timestamp, but rather only a timestamp matching or greater than the index timestamp can be considered valid - proof is left as an exercise to the reader :)

### Reading the index and lazy cleanup

When going to read an index, we can just prune failed updates when we find them (again via coprocessor or from the client), giving us a consistent view from the end-client. Its interesting to note that if you update a key (say, the person moved again), you just need to do a single write for each field you are indexing and and write to the primary table with the same timestamp. We can let the lazy cleanup take care of removing the failed entry when its found. This is a slight optimization for a write-heavy system, but could prove incredibly valuable in the long term (but could be rough in a read-centric system, in which case you would want to cleanup the index when doing a write). 

The careful reader might now ask, "What about partial writes that haven't completed yet? Won't we end up cleaning those too?" If we follow the above methodology, then yes, we will likely end up with false negatives in our index - one of the things we are attempting to avoid to make this indexing scheme all make sense.

However, what one can do is just ensure that if you see a potentially failed write that you don't delete it immediately, but rather add it to the queue of elements to delete, sorted by timestamp. No edit can be deleted before you reach a timeout for a successful write. In the case of batched timestamp retrieval, you just ensure that you wait the batch timeout or do it synchronously on the server (index table writes to the primary table via coprocessor) and writes a special valid flag into the indexed key when a write completes successfully. 

I'm leaving out some of the more intricate things that you can do with the timestamp generator in terms of handling incomplete/partial-failure index rows, leaving it instead as an exercise for the reader.

## Performance - more credible handwaving

If this was a single-system database, then we would update the index table, then go over and write to the primary table. Each lookup is about O(lg(n)), where n is the number of keys, giving us about 2lg(n) time to write a row with a single index (adding 1x for each new index). Lookup times are again 2lg(n) based on any of the indexed fields for that key. 

In the proposed implementation, writes are going to be O(lg(m)), where m = number of keys in memory on each server (for HBase, its actually the number of keys in each region), to the index table, and the same again to the primary table, giving us 2lg(m). Since m << n, but we need to have at least 2 network hops and as many as 4 (depending on which impementation is used - client or server via coprocessor), writes are probably about as fast as the single server system. Keep in mind though, that updates to a key don't need to fix that they don't belong to anymore - we just let the lazy cleanup handle that, amortizing those network hops quite nicely, assuming a write-heavy workload.

We are going to be hurt a little bit on reads - its not all sunshine. We have the same lookup time as for the single system, but since we may have a dirty index, we might have to pull data across the wire that we don't actually want to read. Its possible to setup some sort of constraint/coprocessor system where you return only key-values from the matching row with a timestamp >= the timestamp in the index table. Note we can data transfer across the wire for cleanup even further if we delegate cleanup to a Map/Reduce job in the background, dropping that second overhead in favor of possibly getting some extra cruft over the wire more frequently as a minimal tradeoff.

Even without use of this optimization, I submit client failures causing large amounts of partially-written indexes is very unlikely - at worst you might get a handful of incomplete indexes. This can be calculated as a probability of failure * number of concurrent writes per client * average write size to give you the worst case expected overhead when do a single read. 

On average, this overhead is still just going to be constant value, and for increasingly stable hardware, this approaches a fairly small constant. For example, at the rate of 1 node failure per day, in a thousand node cluster, with 100 concurrent writes and an average write size of 1KB, means a node failure will have <= 100KB of extra data written across the wire an extra time, which on 1Gb ethernet links is 7.649 milliseconds of extra latency for a single read to correct that failure. (99th percentile calculations are left as an exercise to the reader - wow, your really getting a workout today!). 

So, in the end reads are 2lg(n) + small constant time, for n = number of keys, when doing a read + time to transfer data over the wire twice - once to get the data with some excess and once to write the delete into the index (the worst case). This is almost as good, and in non-failure cases exactly as fast, as if the the entire dataset was immutable and we just used the index for speeding up lookups into our primary table. 

This means we can do secondary indexing in a distributed, consistent and network partition tolerant system with only the overhead of going over the wire to do our writes + a constant factor on average - arguably as good as a single system (even with network latency overheads), and in almost every case as fast as indexing into a static dataset. This makes this system _far faaster_ than  using distributed transactions, either through Paxos, optimistic locking or two-phase commit, but just as correct. 

## Wrap up

Its intellectually interesting - though functionally unimportant - to realize that we will have an eventually consistent index table, but further that doesn't matter because we are tracking GUIDs, making each edit essentially immutable (can be deleted, but not updated) and idempotent. There are some niceties when dealing with this state for a large scale system, but that is way out of scope right now. 

We can see though that it might take some time (though using Culvert as a base, not too bad) to build a client-consistent secondary index in a BigTable-like database that is tolerant to failures and optimally fast. Don't think so? I'd love to hear your thoughts in the comments.
