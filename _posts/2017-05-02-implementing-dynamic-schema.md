---
layout: post
title: Implementing Dynamic Schema At Scale
tags: schema, dynamodb, scale, big data
excerpt_separator:  <!--more-->
---
Rehash of the [Dynamic Schema at Scale](/2016/03/09/dynamic-schema-at-scale.html) for how we implemented reading semi-schematized data from a NoSQL. I touch on the query translation and processing of updating schema.
<!--more-->

One of the more innovative things we developed at Fineo are our 'No ETL' infrastructure. Much of this came from the ideas at [stitchfix](http://multithreaded.stitchfix.com/blog/2016/03/16/engineers-shouldnt-write-etl) that engineers shouldn't write ETL, but instead core building blocks enable ETL for the people who care (e.g. data scientists). The Fineo toolkit enables our customers to evolve their schemas at the push of a button, throwing out much of the traditional ETL grunt work. Beyond just eliminating basic transformations, Fineo also enables storing and querying data without any sort of schema! And that means you can move faster than ever before.

Continuing the 'behind the curtain' look at Fineo infrastructure I'm going to dive into how we make dynamic schema possible across our stack.

# Traditional Challenges

Schema changes in traditional RDBMS installations are a non-trivial operations, fraught with peril and often closely managed. Even worse, they can often gate work across multiple teams as you wait for a central data store to be upgraded. On top of that, if you are changing the aspect of any fields (e.g. exchanging fahrenheit for celcius or sending "Temperature" rather than "temp") you have to layer on additional ETL steps before the data can even get into the database. Compound that with any downstream data stores that are maintained to achieve orthogonal goals like analytics v.s. low-latency access to recent data.

Basically, its a giant pain in the ass.

And its not until you have done all this work (which can happen frequently in a fast moving company) that you can even start to look at the data you _would_ send.

# Fineo: Easy Ingest, No ETL

With Fineo, you don't have to do almost any of that work. Schema is defined with a straightforward API, or in our [web app](https://app.fineo.io), and can use the exact same data definitions are you would send to the API.

```
// Example definition
class Event{
	public long timestamp;
    public String metrictype;
    public int value;
}

// Skeleton command line request for creating the definition
$ java -jar fineo-tools.jar create-schema --class Event
```

If you don't define fields before you send them, our backing store (based in NoSQL) transparently stores the data so you access it later.

And then it gets really cool.

You don't even need to do any of that schema management until you have time to get around to it. Instead, you can start querying for fields immediately, just by knowing the names and what type you expect it to be. From there, you can use the whole world of SQL to slice and dice data, so you can quickly and easily get running.

At some point later, you can 'formalize' your schema by defining the expected fields, types and aliases. Formalizing the schema will dramatically speed up any analytics and enable auto-complete queries. However, you can also continue using the alias names for different fields that you had developed before formalizing the schema, so the queries you are already running will continue to run just fine.

# Avro All Around

The root of our schema management process uses [Apache Avro](http://avro.apache.org/). Avro is great - it has schema evolution, field aliasing and self-describing serialization. Sounds like we are done! Just use Avro everywhere.

Ummmm, not so fast.

To start with, you need a way to keep track of all different schemas for each customer. Enter the [avro schema repo](https://github.com/schema-repo/schema-repo), based on some work Jay Kreps did in [AVRO-1124](https://issues.apache.org/jira/browse/AVRO-1124).

The Avro Schema Repo (ASR) is a REST-based service that lets you manage the evolution of schema for a logical 'subject', a collection of mutually compatible schemas (the changing schema of the 'thing' you are managing). The ASR comes with a couple of nice default database adapters - Zookeeper and a local FS (if you poke around, there is also a JDBC-based backend on github).

At Fineo we strive for 'NoOps', choosing instead to rely on hosted services to minimize overhead and automate as much as possible. To that end, we wrote a custom schema store backend on top of DynamoDB. AWS also has relational database (RDS), but is managed by a VM, rather than by the request as with Dynamo, leading to more ops that we really wanted. The trade-off with Dynamo is that we will incur 2-3x write overhead to ensure that we get consistent results [[1](#dynamo-schema-repo)]. Fortunately, schema doesn't need to be blazing fast - the write pipeline is asynchronous.

We are already using Dynamo for our near-realtime store and it has nice NoSQL properties that let us really leverage dynamic field names, so it was a pretty easy choice. Fortunately, ASR has a pretty lightweight requirement on the database adapter and already has a caching strategy, so this was pretty easy to implement (especially using Dynamo's simple ORM tools).

We hope to open source the DynamoDB adapter for ASR soon. Keep an eye out!

# Scan-time, Party Time

At Fineo, we wanted to make it as easy as possible to push any and all data into the database and make it instantly queryable. If we are allowing any sort of data going in, we just need a way to understand what we have. More specificially, we need a way to translate a single column lookup into a lookup for any 'alias' of a column.

 <img src="/images/posts/dynamic-schema/scan-schema-remapping.png">

Part of the power of using a NoSQL store is that we can just stuff in fields without having to touch any extensive DB DDL tools (though our schema management really is "DDL as Metadata"). Since we know the field names, we can then later just query what we expect is in there, and have the database tell us what actually is there.

When reading the data back, we push the expected schema down the processing tree to the very edge node. There we process each row to convert each stored column into the expected column. This way, we can leverage the flexibility of the NoSQL store, but with the usability (e.g. sanity) of a schema.

A user query can start as:

```
> SELECT temp from MY_TABLE
```

and then get translated at the database interface layer to something like:

```
> SELECT temp, temperature, tmp FROM DYNAMO_TABLE WHERE TENANT_ID = 'some-tenant-id'
```

And then we select the column that actually has some data for each row (first available wins) to fill contents of the `temp` field to return to the user.

Our Dynamo extension of the ASR has support for tracking unknown field names and potential types. When we receive events that have 'unknown fields' we update the unknown fields list for that ```Metric``` type in Dynamo and then write the unknown fields into columns by the customer specified name as simple strings. When customers query for fields that have not been formalized they have to provide the expected type of the field by casting it (or just accepting a simple string representation, the most common of denominators).

When we know the field/type, we can pre-cast the fields from the weak-typing done in DynamoDB into the 'real' types the user expects.

# Avro Schema Repository Access

In our drive to achieve 'zero-ops' we throw out the ASR REST layer and just query DynamoDB directly using the ASR api[[2](#elastic-beanstalk)]. It still provides all the caching you would want, but saves us a network hop. Naturally, the trade-off is that we need to be very careful with how we evolve the schema and access patterns, but as a small shop with high visibility into the code effects, we made the choice to simplify ops over later complexity.

Keep in mind that the schema repository has two touch points, (1) the ingest pipeline, where we track new schema and apply existing schema to incoming events, and (2) the external-facing web server, which needs to understand schema to serve reads and for admins to manage the schema.

Since these are stateless services, we could deploy them via ElasticBeanstalk as containers and even replace the direct DynamoDB access with the REST endpoint with minimal changes. For now, we just use AWS Lambda to handle the scalability and availability of the schema service.

## Exposing User Schema

All schema is exposed to users via simple REST requests, managed through an AWS API Gateway (for authentication and authorization) and then sent to a schema AWS Lambda function. This ensures that all schema access is scalable and also easily tested.

We could also use the same API internally, and probably should as we grow, but for now its cheaper and faster (both in development and request time) to directly access the underlying schema store. This means a bit more coordination on the testing side to ensure that there are not breaking changes to how schema is stored, so each lambda function, ingest processing and SQL query execution all work on the same data underlying schema data store.

## Managing multiple entities

As a multi-tenant platform we naturally have to manage multiple customers. Each tenant (customer) is assigned an Id - a tenantID (did you guess it?) - which is then used to lookup the possible schemas for that customer, each assigned a schemaID (I know you didn't guess that one).

Remember how we mentioned that you can rename things on the fly?

Well, that means we can't actually store 'real' names, but instead have to use aliases. These aliases are stored alongside the customer schema so we can manage those aliases directly _as part of the schema_. So the schema for a thing is an instance of a schema.

Thus the schema for a given 'object' (event) is a combination of the tenantID + schema ID + schema alias(es). We have schema for describing a tenant + its known schemas (Metadata) and then each schema has its own schema (Metric). Then for a given type of 'thing' for a tenant, we store instances of the metadata and each metric. This leads to a schema repository that looks like:

  | subject id | schema |
  |:-----------|--------:|
  | _fineo-metadata | Metadata.schema|
  | _fineo-metric | Metric.schema|
  | data production inc. | Metadata.instance|
  | n1 | Metric.instance |
  | n2222222 | Metric.instance |
 {: align="center" width="40%"}

<p/>

Ok, that is going to take some explaining. The Metadata.schema and Metric.schema are actually the following Avro schemas[[3](#metric-fields)]:

{% highlight C %}
 record Metadata {
    string canonicalName;
    union {null, map<array<string>>} canonicalNamesToAliases = null;
  }

  record Metric{
    Metadata metadata;
    string metricSchema;
  }
{% endhighlight %}

These schemas are then used to to understand the Metadata and Metric instances we get per customer. Going back to our example of DPI above, your first level Metadata instance will look something like:

{% highlight json %}
{
  "canonicalName": "n1",
  "canonicalNamesToAliases": {
    "n2222222": ["machine1", "machine1b", "machine1c"]
}
{% endhighlight %}

So the customerId is ```n1```. This customer only has a single schema, with the canonical name ```n2222222```. However, we might get multiple different device name types that are really the same "thing". This is useful when you have devices from different manufacturers that produce different metrics, but are really the same thing.

From there, you can also lookup the schema instance for the device ```n2222222``` (which to the client looks like 
they are looking up machine1 or machine1b or machine1c). That will give you something like:

{% highlight json %}
  {
    "metadata": {
      "canonicalName": "n2222222",
      "canonicalNamesToAliases" : {
        "f1": ["field1"],
        "f2": ["field2", "field2b"] 
      }
    },
    "metricSchema": "\\ some encoded avro schema based on a BaseRecord \\"
  }
{% endhighlight %}

We have a known set of fields that are included in every record, comprising a BaseRecord and its BaseFields:

{% highlight C %}
  record BaseRecord {
    BaseFields baseFields;
  }

  record BaseFields{
    string aliasName;
    long timestamp;
    map<string> unknown_fields;
  }
{% endhighlight %}

For now, its enough to understand that this is the basic building block of an 'object' schema. Shortly, we will discuss how its actually used.

### Wait. What are you keeping track of?

A logical machine (e.g. a thermostat) is actually an instance of a ```Metric```, that has an instance of its own metadata to map canonical field names, keep track of its own name and then store the _schema for the actual customer record_. This allows us to evolve how we define a generic schema for a tenant or metric, as well as evolving how the schema for a given 'thing' looks.

Using schema to define schema... followed by big of pile of turtles to the bottom :)

Note that Avro's standard aliasing only applies to records, which means that every field becomes its own record instance, which quickly gets to be a pain to manage and also prevents easy alias logic reuse. I'm not saying you couldn't do it, it just gets to be a pain (left as an exercise to the reader).

## Building schema - modern DDL

The schema for a given field is programmatically built through our DDL Apis. By using an instance of the ```Metadata``` we can dynamically rename fields without actually changing any underlying data. Eventually, we also want
to do dynamic type conversion and lazy ETL.

Each ```Metric``` instance's metricSchema (I know, the naming is a touch confusing - I'm open to suggestions) is actually an extension of the ```BaseRecord```. Each event in the platform is expected to have a couple things when it reaches the 'write' ingest processing stage:

 * a timestamp
 * a customer specified alias (which we remap to a canonical name)
 * some number of unknown fields

Going back to our example of DPI, they brought a new machine online which has a couple of metrics: temperature, pressure and gallons. After connecting it to the platform, we will end up with a record that looks like:
{% highlight json %}
  {
    "source": "new machine",
    "timestamp": "January 12, 2015 10:12:15",
    "temperature": "15",
    "pressure": "4",
    "gallons": "5"
  }
{% endhighlight %}

Which gets remapped via the [Fineo ingest pipeline] to a simple ```BaseRecord``` instance:
{% highlight json %}
  {
    "alias": "new machine",
    "timestamp": "1421057535000",
    "unknown_fields": {
      "temperature": "15",
      "pressure": "4",
      "gallons": "5"
    }
  }
{% endhighlight %}

The ```unknown_fields``` then get stored as simple strings in DynamoDB, which we can read later (through some gymnastics) _without having defined any schema or types_. At some point later, an user goes in and formalizes the schema to types that we talked about. The 'extended ```BaseRecord```' and machine ```Metric```instance then looks like:

{% highlight json %}
{
   "metadata": {
      "canonicalName": "n2222222",
      "canonicalNamesToAliases": {
        "f1": ["temperature"],
        "f2": ["pressure"],
        "f3": ["gallons"]
      }
    },
    "metricSchema" :
      "record: BaseRecord {
        BaseFields baseFields;
        int f1;
        long f2;
        int f3;
      }"
}
{% endhighlight %}

Since we are backing everything by Avro, we can cache schema until we find it is out of date, and only then request a new one. Further, by storing all the fields by tenant and schema, we have a very high throughput, multi-tenant access that probably doesn't need much of a cache, which backed by DynamoDB gives us highly scalable schema evolution.

## Future Work

While this gets you pretty far, we do see somethings that we think customers would find helpful:

 * field type prediction
 * dynamic field typing, so you can change the type of data
 * advanced sanitization and transformation
 * missing field alerts

Want to learn more about the [Fineo] architecture? Check out the next post in the series: [translating SQL queries](/2017/05/05/translating-sql-queries.html).

#### Notes

##### 1. Dynamo Schema Repo
We can actually be a bit lazier here and not read/write with full consistency, instead relying on the mutually compatible evolutionary nature of Avro schema. We should be able to step through old versions to read data from data serialized with an older schema. In fact, we can keep track of which schema number (schema-id) the data was written with and just use that schema to deserialize data.

##### 2. Elastic Beanstalk
We could actually use [AWS Elastic Beanstalk](https://aws.amazon.com/elasticbeanstalk/) to do a lot of the ops for us in deploying the web service. However, they is still another moving part. It gets us nice separation and ability to evolve schema, but that seemed minor gains _right now_ compared to the overhead of running another service. Of course, as Fineo grows this will not always be the case and the advantage of using a more SOA style architecture will be increasingly compelling.

##### 3. Metric Fields
We also have the ability to 'hide' fields associated with a machine. This allows us to do 'soft deletes' of the data and then garbage collect them as part of our standard age off process.

##### 4. Realtime
For some definitions of realtime. Currently our ingest pipeline is less than 1 minute, though we have extensions that
 allow querying on data within tens of milliseconds of ingest. Talk to [Jesse](mailto:ceo@fineo.io) if that is 
 something you are interested in.

[Fineo]: http://fineo.io
[Parquet]: http://parquet.apache.org
[Drill]: http://drill.apache.org
[Spark]: http://spark.apache.org
[Fineo ingest pipeline]:
