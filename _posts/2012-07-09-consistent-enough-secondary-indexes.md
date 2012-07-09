---
layout: post
title: Consistent Enough Secondary Indexing
location: San Francisco, CA
description: Distributed systems inherently trade-off consistency or availability, making it very difficult (but doable) to implement secondary indexes at scale
tags: hbase, bigtable, second, secondary index, consistency, availiabilty, CAP Theorem
---
In databases, data is organized into tables, sorted by the 'primary key' of each data row. The primary key is generally either a globally unique id (GUID) or so other uniquely identifying information for that row. 

For instance, in a database of people, you could use social security numbers (SSNs) as the primary key each person-row. Then to find a person by SSN, you can then do O(lg(n)) lookups and find that row. However, suppose you wanted to lookup the person by their address - maybe you want to find all the people living at '123 Jump Street'. With the current table setup, you would have to scan the _entire person table_, looking at each record to see if that person lives at '123 Jump Street' - potentially huge, time consuming query. 

The idea behind secondary indexes is that we 'index' the address field of all the people in our database into another table. The primary key of the secondary index table is just addresses and then it stores all the primary keys (social security numbers) of people living at that address. We are trading space for speed and in very large queries, this tradeoff is entirely acceptable. This means that it becomes very fast lg(n) to find the indexed row and then lg(n) again to do the lookup in the primary table. 

In our people database example, to find all the people living at '123 Jump Street' you can the just jump right to that row in the index table, and get the all the primary keys in the people table that have that address. Then to find all the people living at '123 Jump Street' you can the just jump right to those keys, giving you the information for all the  people directly from the 'people' table. This is vastly more time efficient that doing a scan of the entire people table, looking at each record to see if they live at '123 Jump Street'. 

Creating secondary indexes in an RDBMS is a very natural fit - every time you update a row (either a new row or updating an old one), it is just turned into a transaction where you are doing an update to the index everytime you do an update to a row that is indexed. For example, if a person moves, we update that information in the 'people' table and at the same time update address index table. Since this is a full transaction, if the client or server fails halfway through the transaction, we will never have an inconsistent state, so reading the location row always gives us the latest information.

The problem with this approach for a distributed system is that transactions are _hard_ when spread across machines. To be completely safe they require a Paxos-like protocol to complete, which can be very costly timewise. You can play some tricks with optimistic locking to get good overall performance, but its still very hard to ensure that even 90th percentil times that are anywhere close to the average.  

In the past I've worked on some code to do secondary indexing ([Culvert](http://github.com/jyates/culvert)) for a BigTable like system, but the indexing was of secondary importatnce (no pun intended), to the rest of the work of doing SQL over BigTable like systems. Culvert is almost there in terms of overall correctness, but trades-off full consistency for speed and ease. We decided that a dirty index (false positives) and fast writes were more important than having a fully consistent view of the index. 

There are ways to do the latter, and the guys over at [Lily] (http://www.lilyproject.org/lily/about/playground/hbaseindexes.html) have done a terrific amount of work to make it possible. However, Lily has a lot of moving parts (secondary servers, a full write ahead log, etc.) that make it inherently hard to use, fragle and slow - somewhere on the order of 100's of writes per-second in HBase, which natively can do millions of writes/sec.  Don't get me wrong - they are great guys and are doing great work, but we can do better.

There are a couple of pieces that we need to put together to enable consistent secondary indexes. The first key realization is that we don't need to be _transactional_ - indexing can be an idempotent operation if we bind the writes to a timestamp (the second part).

Let that sink in for a moment... it means we can essentially 'cheat' (traditional) indexing and still always be right (enough). Its known that secondary indexing in a distributed environment is inherently easier that full transactions, but its rarely articulated why its easier. Idempotence allows us to retry without concern about currentl running operations or worry about previous effects. This is huge. A game changer.

We then have two  major concerns - making sure we never get a 'wrong' answer from the index (even if the client or server crashes) and making an indexed writes _fast_. If we didn't care about the latter, we could do two-phase commit with a WAL and get correctness (if that's good enough, just use Lily), but we are going to take a signifcant latency hit due to the high number of writes and further - depending on the workload - could be highly contentious with respect to locking (for multi-row writes). 

Instead, we split the effort by making the client a little bit smarter and adding some more manipulation on the server-side. 

## Example and proof by hand-waving

Let's walk through the implementation via an example. 

Architectually, the first piece we would like to have is a globally unique id generator, allowing each client to apply the same write number to each batch of writes. Something like this is discussed in [Percolator] (http://research.google.com/pubs/pub36726.html), and its fairly trivial to implement something similar over HBase using the increment operation (I've recently done it and hope to open source it in the near future - check back on my [github] (http://github.com/jyates) page).

Going back to the people example, say we want to add a new person to our database with an index on the address. This is just a simple inverted index example, but you could do smarter things like pre-joins, etc in the index with the nearly exactly same methodology.

The client then has to do a couple of things. First, it gets a write number from our global generator to apply to this batch of writes - this allows us to reason between the index state and the primary table state cleanly, but is still possible if you set write timestamps from the client. 

Once it has a write number, the client _first_ writes to the _address index table_ - the address row gets updated with the person's SSN. It looks something like:

{% highlight bash %}
    Address     |      SSN     |  timestamp
123 Jump Street |  111-22-3333 |      2
                |  333-44-5555 |     14
{% endhighlight %}

supposing that the someone with SSN=111-22-3333 already lives at 123 Jump Street and we added person SSN=333-44-5555. After successfully writing to the index table, the client then writes the same person to the primary table:

{% highlight bash %}
   SSN      |     Address     | timestamp
333-44-5555 | 123 Jump Street |    14
{% endhighlight %}

Then when another client attempts to lookup who lives at 123 Jump Street and will see two SSNs, the one that was already there and the one that we added. Secondary index built and working? Check.

## Failure Situations
While this indexing scheme works when no component fails, things get interesting when we deal with failure situations, which are very likely on the commodity hardware that runs the largest clusters in the world. 

Suppose the client fails before either write hits the wire - no problems, we still have a consistent system (okay, that was a gimme). Same story for if the client if both the writes succeed - another easy one.

Note that we can never get into a situation where there is a write to the client table, but not to the index table becuase we always guarantee that the index table succeeds before writing to the primary table. You can do this by having the client write to the index table, waiting for success and then writing the primary table or via an indexing coprocessor that writes the indexes to all the tables before it writes to the primary table - these are just implementation details (for the record the latter would be faster and clearn from a client perspective, but a bit more difficult to implement). Either way, you will _never have false negatives_ in the index table, you will only get false positives in the worst - partial failure - case.

False positives occur when the first write - to the index table - succeeds, but the write to the primary table fails. This puts us in a little bit of an odd situation because the index table says a row should exist, but the primary table doesn't have that edit. We know the edit belongs to that index because of two key points: 

 * both writes have the same timestamp  
 * each timestamp is unique (via the timestamp generator), so it must have come from a partially-successful write

allowing us to ignore the failed edit. Basically any row in the primary table with a key who's latest update doesn't match the expected timestamp (allowing us to keep multiple versions of the table back), can be considered a broken link and lazily cleaned up. Note that we don't actually need to use the latest timestamp, but rather only a timestamp matching or greater than the index timestamp can be considered valid - proof is left as an exercise to the reader :)

### Reading the index and lazy cleanup

Reading becomes a bit more complicated when dealing with failures. If there are no failures, each write to the index will create a row (specifically, a row key, column family, column qualifier, timestamp, value tuple) that corresponds to the written index row. When we go to retrieve the primary row (the set of all key-values for that row), we have to consider which timestamp versions of each key-value to take since a Delete may have deleted specific versions of the key state between when the index was written and when we retrieve the key. 

Note that we will never find the row from the index if the row (or the column that was indexed) was deleted since we _always_ update the index before the primary table. This means that at least the primary key and the information for the indexed value will be correct when we do a read. 

Well, there is a case when it won't be exaclty correct - we could actually have written the index and then failed to write the client. This can also happen if we are just slow to see the primary table write while reading the index table. In this case, we could proactively update the primary table with the correct information for that indexed value and timestamp. However, easily leads to corrupted data in the primary table because we only know the indexed value, not the fully primary key. The right thing to do then is to assume that the client write failed and mark that index for deletion (we don't delete it right away to avoid slow primary table race conditions - we'll get back to this point). 

A read then will can always return the latest state of the row in the primary table, if it has the indexed column. This is obvious because if we find the row in the index and that indexed value is found, the row is properly indexed and we want to get the current state of the entire row. We still need to consider the rest of the information associated with primary key we found from the index. The safest way to retrieve the row from the primary table is to return only those columns with a timestamp equal or greater than the indexed timestamp (for columns not equal to the column we used in the index - if that column is newer, then our index is out of date and we can clean that up on the way out). Let's go back to the person example again, and see if this actually works out.  

Suppose, our theoretical person with SSN=333-44-5555 is also named John Smith and has a cat. His entry in our primary table would look something the following, if we put all that his information at the same time:

{% highlight bash %}
   SSN      |     Address     |   Name     |    Pets   |  timestamp |
333-44-5555 | 123 Jump Street | John Smith |    cat    |     14     |
{% endhighlight %}

Doing a lookup into the primary table then works as expected. However, consider the case where we knew John's name and that he had a cat, but not his address at timestamp 7, but then later, at timestamp 14, learned his address. His row in our primary table then looks look something like this:   

{% highlight bash %}
   SSN      |     Address     |  --------------------  |  timestamp |
333-44-5555 | 123 Jump Street |  --------------------  |     14     |
                              |   Name     |    Pets   |
                              | John Smith |    cat    |     7      |
{% endhighlight %}

If we just return the columns with timestamps greater than the edit we wrote, then we will actually miss most of the information with John. The same applies to returning columns that have timestamps greater than the one we indexed. The only time we shouldn't return the latest values is if the column we indexed has a timestamp that doesn't match the index in the index.
 
Its interesting to note that if you update a key (say, the person moved again), you just need to do a single write for each field you are indexing and and write to the primary table with the same timestamp. We can let the lazy cleanup take care of removing the failed entry when its found. This is a slight optimization for a write-heavy system, but could prove incredibly valuable in the long term (but could be rough in a read-centric system, in which case you would want to cleanup the index when doing a write). 

The careful reader might now ask, "What about partial writes that haven't completed yet? Won't we end up cleaning those too?" If we follow the above methodology, then yes, we will likely end up with false negatives in our index - one of the things we are attempting to avoid to make this indexing scheme all make sense.

However, what one can do is just ensure that if you see a potentially failed write that you don't delete it immediately, but rather add it to the queue of elements to delete, sorted by timestamp. No edit can be deleted before you reach a timeout for a successful write. There are a couple ways to cleanup the index in the background. 

1. The easiest way is to just periodically (e.g. daily) run a MapReduce job that compares the state of the primary table to the index table and removes index entries that you are should should have been commited. You can be sure they are committed by, for example, only cleaning up broken indexes with timestamps older than a day. If you are using a timestamp generator, it will probably push batches of timestamps, where each batch has a TTL, which you can then use to check cross-table consistency. 
2. Using a daily job will likely cause a lot of overhead where you are looking up data that you really shouldn't and can get around via doing cleanup when you find the broken link. This costs you an extra roundtrip with a delete for that index cell, but this is a single operation that can be done _asynchronously_ to the primary client and prevents other clients from finding that broken link in the index. There are futher two options for doing this on the fly, depending on your implementation:

    * client-side - in this implementation, the client gets the primary keys after reading the index table, then looks up each key in the primary table. The results are then filtered at the client (as mentioned above) to ensure the end-user only sees the correct key-values. Any borked index entries can then be cleaned up in a background request from the client.
    * server-side - when do an indexed lookup, the index table actually goes to the primary table to retrieve the results and then validates those against the index before pushing those values back to the client. This has an network hop for the primary table (which has a bulk of the information), but saves the return of the primary key list to the client (a relatively small overhead).

Note that any of these read and cleanup methods can be used with indexing of writes through either the client or server and can lead to some really nice implementations, that are more or less useful, depending on your environment. Imagine a 4x4 grid with reads on one axis, writes on the other and client/server as the 'unit' of the axis.

{% highlight bash %}
          | client   |   server  |  <--- Reads 
   client |    1     |     2     |
   server |    3     |     4     |
 Writes
{% endhighlight %}

Lets break down what you get in each quadrant:
 * 1 - the client writes the index and then writes to the primary table. On reads, the client first gets the primary keys from the index, then gets the rows from the primary table and filters out incorrect rows. These incorret writes are asynchronously removed from the index table.
 * 2 - same writes as (1), but you only query the index server and let the server take care of retrieving the primary keys, filtering them, passing them back to the client and then locally updates the index links.
 * 3 - The client just writes the primary keys to the index tables, and one table is picked as the 'leader' table and writes all the data to the primary table. In this case, we can actually mark all the indexes as completed or not on the leader table. On reads, the client still asynchronously updates the index table used in the read.
 * 4 - Writes are the same as (3), but then reads are done through the index table, as in (2), with all the pruning and updates that implies.

There isn't an immediately apparent answer for which quadrant is the right one to go with every time. In fact, it is likely to depend on your cluster; specifically, you will need to consider (1) the bandwidth between the clients and the servers, (2) bandwidth between servers, and (3) the flakiness of clients. Depending on the BigTable implementation you can you, there are some more tricks you can play with things like filters, Iterators and/or Coprocessors for optimizing where the filtering, updates, etc. happen to get even better performance.

## Performance - more credible handwaving

If this was a single-system database, then we would update the index table, then go over and write to the primary table. Each lookup is about O(lg(n)), where n is the number of keys, giving us about 2lg(n) time to write a row with a single index (adding 1x for each new index). Lookup times are again 2lg(n) based on any of the indexed fields for that key. 

From the quadrants above, lets look at quadrant 1 - the client writes first to the index table (assuming a single index, but its trivial to multiply for other indexes) and then writes to the primary table; on reads we query the index to get the primary keys, the data from the primary table, filter incorrect results and then asynchronously push them to the index table. 
 
In the proposed implementation, writes are going to be O(lg(m)), where m = number of keys in memory on each server (for HBase, its actually the number of keys in each region), to the index table, and the same again to the primary table, giving us 2lg(m). Since m << n, but we need to have 2 network roundtrips (one for the index, one for the primary table), writes are probably about as fast as the single server system - ignore the man behind the curtain. Keep in mind that we don't need to update old indexes when we change a key - we can let the lazy cleanup handle that, amortizing those network hops quite nicely and speeding up a write-heavy workload.

We are going to be hurt a little bit on reads. We have the same lookup time comparable to a for the single system: lg(n) for the index + lg(n) for the primary table + primary table round trip (so far the same) + roundtrip for the index. This last roundtrip is a fairly small value given that we are just pulling across primary keys, not the entire primary row. 

Since we may have a dirty index, we might have to pull data across the wire that we don't actually want to read. Its conceivable to use a coprocessor here that only return only key-values from the matching row that are correct, saving you that data over the wire, but not the lookup and roundtrip. Even without use of this optimization, I submit client failures causing large amounts of partially-written indexes is very unlikely - at worst you might get a handful of incomplete indexes. This can be calculated as a probability of failure * number of concurrent writes per client * average write size to give you the worst case expected overhead when do a single read. 

On average, this overhead is still just going to be constant value, and for increasingly stable hardware, this approaches a fairly small constant. For example, at the rate of 1 node failure per day, in a thousand node cluster, with 100 concurrent writes and an average write size of 1KB, means a node failure will have <= 100KB of extra data written across the wire an extra time, which on 1Gb ethernet links is 7.649 milliseconds of extra latency for a single read. The correction is dones asynchronously, and transparently to the client, so we can ignore that overhead. (99th percentile calculations are left as an exercise to the reader - wow, your really getting a workout today!). 

So, in the end reads are 2lg(n) + small constant time, for n = number of keys in a region (<< total data set), when doing a read + time to transfer all the data over the write. In the worst case, you will have to do async update to the index, but that can either be batched or just done as its own RPC without affecting reads. This is almost as good, and in non-failure cases exactly as fast, as if the the entire dataset was immutable and we just used the index for speeding up lookups into our primary table. 

This means we can do secondary indexing in a distributed, consistent and network partition tolerant system with only the overhead of going over the wire to do our writes + a constant factor on average - arguably as good as a single system (even with network latency overheads), and in almost every case as fast as indexing into a static dataset. This makes this system _far faaster_ than  using distributed transactions, either through Paxos, optimistic locking or two-phase commit, but just as correct. 

## Wrap up

There are a lot of potential optimizations you make on top of the proposed implementation - utilizing things like filters, coprocessors, and iterators more efficiently to minimize data across the wire, pre-joining in the index to avoid going to the primary table entirely, etc - that can lead to even faster secondary indexing. 

However, we can still to incredibly performant, correct indexes. It might take some time to build an super-optimized implementation of client-consistent indexes, but there are no apparent technical problems; its likely that the simple solution will be performant enough for all but the most demanding use cases.

Think I'm crazy or fully of it? I'd love to hear your thoughts in the comments.
