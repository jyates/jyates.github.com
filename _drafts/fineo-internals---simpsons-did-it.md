---
layout: post
title: Fineo Internals - Simpsons Did It
location: San Francisco, CA
subtitle:
tags: aws, time series, scalable, EMR, stream, processing
---

Last week a couple of articles came out, highlighted by the wonderful [Hadoop Weekly](https://www.hadoopweekly.com/), specifically one by Netflix talking about [their data pipeline](http://techblog.netflix.com/2016/02/evolution-of-netflix-data-pipeline.html) and dtrapezoid talked about a [simple schema for time series tracking on Cassandra](http://dtrapezoid.com/time-series-data-modeling-for-medical-devices.html). This is, perhaps not so ironically, a lot of what [Fineo] has implemented for our backend. Basically a case of the 'Simpson's did it' (the Simpsons have been on the air so long, they have done every single gag and bit).

<div align="center">
<iframe width="420" height="315" src="https://www.youtube.com/embed/iDuMp2kDxos" frameborder="0" allowfullscreen></iframe>
</div>

Now, as with all Simpsons instances, the key comes in the differentiators. Most obivously, [Fineo] is a SaaS provider - you get all the great things you get with a flexible ingest pipeline and fast, IoT centric storage, without all the overhead of actually running it yourself. While a Netflix-style pipeline may not be presented to you directly as a customer, you get the rapid development, testing and iteration a staged, streaming architecture.

Fineo is also built almost exclusively on the amazing building blocks in AWS. Instead of Cassandra, we leverage DynamoDB - a bit improvement over the notoriously hard to run Cassandra. Instead of Kafka, we use AWS Kinesis. Finally, for stream processing we leverage AWS Lambda. 

Oh, you can also make it end-to-end encrypted with the push of a button. And access control? Yeah, we provide fine-grained, role based access control.

We also take great care to ensure data is never, ever lost. All events are stored in with many duplicate copies across several data centers to ensure the highest availability, and enabling us to restore data back to very specific points in time.

These days, those sort of things is really table stakes.

Where [Fineo] really shines is three places:
 1. SQL everywhere
 2. Low latency query and alert
 3. Dynamic schema at scale

# SQL Everywhere

SQL is the defacto data access standard for analysts. By supporting SQL, and jdbc connectors, we provide easy integration with the host anayltics tools that you already know and love.

Traditional SQL, and therefore traditional tools, do not currently support the idea of doing _streaming_ analysis. In fact, this is still an area of active development in the open source community (see [Calcite](http://calcite.apache.org/) and [Flink](https://flink.apache.org/)). At [Fineo] we are actively involved in the developing SQL Streaming Standard and have made available some early Streaming SQL capabilities via our platform.

## Fineo Platform - Low Latency Query and Notifications

Our data platform is easily accessible as a web application that lets you dynamically query you existing data and data as it arrives. With a innovative query federation system, we can run your query across the stream, DynamoDB (low latency, key-value access) and across a columnar store (analytics optimized access) with response times in the *10s of milliseconds*, making it viable for nearly any application.

In our application you can do deep, adhoc analysis across your entire data set, or slice-and-dice it wildly fast. Those same analytics can then be transformed into actionable, realtime alerts. Now your ops engineers can have the same tools to do investigations as the analyts use to create the warnings in the first place.

Integrating further with existing AWS services means we can send SMS and email notifications, as well as custom API endpoints, for alerts right when they occur.

# Dynamic schema at scale

This is some of the coolest work in [Fineo]'s platform. Traditionally, you would have to define a schema before you can query your data. This is an extra hurdle to data integration and red tape you don't need.

From the ground up we are built to be multi-tenant, meaning we have a more rich key-space than proposed by dtrapezoid. Further, [Fineo] also enables you to send and query data immediately, as long as you know what you are looking for. 

Later, you can formally define the schema. Since we know what fields you sent in each event and how you have been querying it, we will suggest fields and their types. Even better, you no longer have to be concerned about the same field having multiple different names. We can dynamically map two (or more) different field names into the same logical name. The only reason you need to approve schema changes is so we speed up your queries. Until you specify types we have to treat everything as strings and do matching and conversion from there.

In another post I'll talk about how we actually go about doing dynamic schema at scale.

# Wrap Up

Raelly good ideas never seem to be uniquely developed - also quite a few bad ones - and such seems to be the case here. Our ingest pipeline looks a heck of a lot like Netflix and our DynamoDB schema looks similar to a common IoT style usecase. However, we have some twists that make [Fineo] eminently attractive: SQL access, enterprise security and availability, low latency query and dynamic schema. I don't think the Simpson's did this one.
 
[Fineo]: http://fineo.io
