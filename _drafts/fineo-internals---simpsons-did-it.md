---
layout: post
title: Fineo Internals - Simpsons Did It
location: San Francisco, CA
subtitle:
tags: aws, time series, scalable, EMR, stream, processing
---

I'd like to talk a bit about the AWS-focused ingest pipeline that we developed at [Fineo]. Not too ironically, its very 
similar to the pipeline that Netflix discussed by in a [recent article](http://techblog.netflix.com/2016/02/evolution-of-netflix-data-pipeline.html)
, highlighted by the wonderful [Hadoop Weekly](https://www.hadoopweekly.com/). This was almost a classic case of "the 
Simpson's did it". 

<div align="center">
<iframe width="420" height="315" src="https://www.youtube.com/embed/iDuMp2kDxos" frameborder="0" allowfullscreen></iframe>
</div>

Now, as with all Simpsons instances, the key comes in the differentiators. Our pipeline is very similar to the one at 
Netflix, but is also leveraged to enable real Enterprise SaaS requirements: end-to-end encryption, backups, and validation. 
Additionally, our design allows for easy, rapid prototyping and deployment of new components.
 
<img src="/images/posts/fineo_internals/ingest.png" align="left" width="216" height="280" Hspace="30" Vspace="10">

We leverage a host of AWS services for a couple of reasons: (1) as we scale up, cost scales with us and (2) operational burdens 
are nearly zero. Instead of storing data in Kafka, we leverage Kinesis, which has very similar semantics. Kinesis also 
integrates with a variety of end points - web APIs and Amazon's new IoT service which we look to adopt soon.
 
A series of AWS Lambda functions then process the records off the raw ingest Kinesis stream. The first converts the raw 
record in a schema that we understand or kicks it out to an error stream. The 'valid' records are then send onto two places: 
a archive (used for backup and scalable replay) and the 'Staged' Kinesis stream. The Staged stream is then processed by the 
'Staging Ingest' Lamba function. Similar to above, error records are kicked to Firehose, along with another archive.
Additionally, this stage also writes to 
Dynamo DB, so it can serve near-line queries. Because each event is unique we don't have to worry too much about Dynamo's 
eventual consistency, though we can turn up the consistency as needed (e.g. for historical corrections).

The endpoint S3 "staged archive" location is then processed with an EMR Spark cluster to do a few things:

 * deduplicate records
 * extract schema changes 
 * format records for ingest into Redshift
 * archives raw records to S3 Glacier (nearline backup)

From there we periodically bulk load into Redshift from the output S3 files after processing via EMR. Note, we can be lazy 
about this since the data is already served from the nearline storage. The schema changes get sent to the customer to validate so we can formalize the 
schema for records. Note, we already store the records, before formalizing the schema, in Dynamo. With some slight smarts we 
can query the records back out again, without knowing their types or 'official name' (more on this in a follow up blog post).

And that's the whole pipeline! So what does all that buy us?

 1. Rapid development and ease reading from the Kinesis Streams, without impacting customers
 2. Continuous, staged backup
 3. Long SLAs on Redshift ingest

Note, we can just point our ingest pipeline at an S3 file and just as easily handle batch processing records - handy for more
"traditional" companies that do bulk exports.

# Firehose Benefits

Firehose has a couple of key benefits. First, it acts as a low operational overhead backup system for relatively little cost.
S3 is hightly durable ([99.999999999% durability](https://aws.amazon.com/s3/faqs/#data-protection)), but also has built in 
encyption, hitting many of our core requirements.

Since we Firehose at each stage, we also get infinite replay for each stage. This is necessary when Kinesis only keeps events 
for a certain time, but also useful to handle cases of data corruption issues from a given stage - we can just deploy a new version
and replay from the previous stage's archive. Its also nice if we want to do more extensive testing.

Each stage can also see two main types of error we can see - ingest/customer errors from bad data and 
commit/processing errors. For each error type we write them to a different Firehose stream. This lets us then tie in AWS 
Notifications to alert when we get an error (as an S3 file). This can either be a notification directly to the customer or 
waking up the Ops team in the middle of the night.

**Pro Tip:** _the default firehose limit is only 5 streams. With 2 stages, each with an archive and two different error streams,
you already exceed that limit. Its possible to combine your error streams and then do some post processing in EMR to separate
 the components... or you can just request a limit increase - Amazon is pretty responsive :) Just make sure you plan for 
 production and dev!_  

With basically no operations, Firehose is an incredibly useful tool we have leveraged in a couple of ways to make our 
infrastructure both highly fault tolerant and highly testable. There are a couple of open source projects that can do the 
equivalent work of Firehose - batching up writes and dumping to a DFS (i.e. HDFS); Firehose is nice in that you don't need to
run any of your own infrastructure. 

# The Pitch 
As a SaaS provider [Fineo] gives you all these great things you would want with a flexible ingest pipeline and fast, IoT 
centric storage, without all the overhead of actually running it yourself. While a Netflix-style pipeline may not be presented 
to you directly as a customer, you get the rapid development, testing and iteration a staged, streaming architecture.

Beyond the usual time series monitoring services we are also foremost an enterprise company. With the push of a button you 
can encrypt your data from end-to-end.  And access control? We provide fine-grained, role based access control.

Beyond the standard enterprise-y features, [Fineo] really shines in three places:

 1. SQL everywhere
 2. Low latency query and alert
 3. Dynamic schema at scale

The first two are pretty cool. Being able to use SQL everywhere means quick adoption across the company and natural, powerful
 query semantics. On the Fineo Data Platform we take it one step further, adhoc analytics can be turned into a real-time 
 monitoring alert with the push of a button. Then, if that alert goes off, you can do deep investigation with the same SQL 
 tools. This power is accessible both through the web application, a JDBC driver or programatically through our web API.
 
Built on cutting edge stream processing technology we can respond to queries on the stream in milliseconds. Then a fast, 
scalable KeyValue store enables your near-line analytics. Finally, we also store data in a scalable columnar store which 
allows you do complex analytics blazingly fast.

Some of the most interesting work in [Fineo]'s platform is around schema management. Traditionally, you would have to define a 
schema before you can query your data. This is an extra hurdle to data integration and red tape you don't need.
From the ground up we are built to be multi-tenant, meaning we have a more rich key-space than proposed by dtrapezoid. 

[Fineo] also enables you to send and query data immediately, as long as you know what you are looking for. We will 
quickly notice when you have new events (that EMR job I mentioned above) and alert you so you can either handle it as an error 
, merge it into your current schema, or create a new event type that you want to monitor. Since we know what fields you sent 
in each event and how you have been querying it, we will suggest fields and their types.

Even better, you no longer have to be concerned about the same field having multiple different names. We can dynamically map 
two (or more) different field names into the same logical name. The only reason you need to approve schema changes is so we 
speed up your queries. Until you specify types we have to treat everything as strings and do matching and conversion from there.

In another post I'll talk about how we actually go about doing dynamic schema at scale.

# Wrap Up

Really good ideas never seem to be uniquely developed - also true of quite a few bad ones - and such seems to be the case here. 
Our ingest pipeline looks a heck of a lot like Netflix and our DynamoDB schema looks similar to a common IoT style use case. 
However,we have some twists that make [Fineo] eminently attractive: SQL access, enterprise security and availability, low latency 
query and dynamic schema. 
 
I don't think the Simpson's did this one.
 
[Fineo]: http://fineo.io