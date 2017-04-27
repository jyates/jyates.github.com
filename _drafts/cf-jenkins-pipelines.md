---
layout: post
title: Deep Testing with Jenkins Pipelines and Cloudformation
location: San Francisco, CA
tags: jenkins, cloudformation, template, aws, continous integration, ci, lambda, pipelines
---

Getting a robust continous-integration suite was an early priority at Fineo. By spending some upfront time getting good infrastructure in place we could move dramatically faster down the road; with a distributed, micro/nano-service based architecture, indepth testing is a must.

In the beginning, this started out as just a bunch of unit and simple integration tests kicked off by Jenkins. It then got more extensive as we added in a suite of full, local tests using similar-to-prod resources. Finally, we moved onto doing full-blown, automated, production-like deployments with extensive, life-like test suites. By leveraging [AWS Cloudformation] templates we could easily replicate production, giving us confidence that things would work. Once there, we could confidently automatically deploy to production - there were no other tests to run that would give us more confidence.

Then we started to get fancy.

Debugging test runs was still a pain, involving manually parsing through kilobytes of logs and ugly messages delineating stages, like:

```
---- DEPLOY Started ---
** Stream Processing Deploy started **
...
** Stream Processing Deploy COMPLETED **
--- DEPLOY COMPLETE ---
```

We were also storing all the jobs as chains of scripts - Bash, Ruby and Python - which starts to get unwieldly very quickly.

Enter Jenkins Pipelines.

Jenkins 2.0 brought Pipelines support - a  Groovy DSL for job definition and a good looking UI for tracking steps. Still a bit rough around the edges, but a huge step up from chaining bash scripts and reading terminal output.

Quickly after discovering Pipelines, we started to slowly migrate jobs over to the Pipeline framework. Some of the jobs took tens of minutes, providing the perfect opportunity to work on the conversion of the long-running job.

# Building to End-To-End Testing

The first stage in our build process is local unit and integration tests, for each component. Most of the infrastructure is Java based, and built by Maven, so this is as simple as `mvn clean install`. Often this will trigger downstream jobs to also build. There are a couple of 'sink' jobs - jobs that don't trigger a downstream job - that we monitor to kick off the local end-to-end testing.

The local end-to-end testing stands up local versions of all the AWS services we leverage - DynamoDB, mock Lamdba, Spark - and runs a set of tests that leverage the system in a 'real world' like cases as we can find, checking things like ingesting, reading, schema management and batch processing.

<img src="/images/posts/cf-jenkins-pipelines/local-e2e.png">

Each test is its own stage in the Jenkins Pipeline, allowing us to easily see progress in the test suite.

Supposing the end-to-end testing completes successfully, we then kick off the production like testing pipeline. This has a two main phases: tenant-specific deployment and any-tenant deployment. The tenant specific validates that only a specific api key can be used to access the data, while the non-specific merely ensures you have a valid api key.

<img src="/images/posts/cf-jenkins-pipelines/auto-deployment.png">

In both the tenant-specified and non-specific cases, we do a _complete deployment of the software stack, just like in production_. The only difference to production is the name of components and where they write data. A huge boon, and really the best/only way to do this on AWS, is AWS Cloudformation which allows you to provide templates of your resources and semi-automatically upgrade them as the templates change.

<img src="/images/posts/cf-jenkins-pipelines/full-stack-test.png">

Cloudformation is great, as long as you stop trying to manually manage the Cloudformation instantiated resources (just don't, it _will_ break things). Templates ensure you get the same thing every time and don't have snowflakes.

# Templating the Templates

Since the Cloudformation template files are just text (JSON or YAML), we decided to leverage them for test generation by templating the templates. Specifically, we used [Liquid] templating language, as most of the test infrastructure is also ruby. This ended up being a huge time saver as we could reuse a lot of common definitions (i.e. s3 bucket locations or API gateway monitoring options) and have a common way to define test infrastructure (i.e. all s3 test paths start with /test).

In fact, we also used Liquid templating in generating the API Gateway definition files, invaluable when attempting to do things like create a common response language across multiple apis (and you don't just jam all the endpoints into a single api, right?).

Using our generated templates, a full production-like deployment can then be stress tested across a variety of real-life use-cases. And since we are a time-series platform, and its important to [drink your own champagne](https://en.wikipedia.org/wiki/Eating_your_own_dog_food), we capture the amount of time it takes to perform each of the tests and feed that data back into our platform, as well as the CPU/Memory consumption of our Jenkins Server. Then if the time for any component is wildly different from the historical values (avg, 75%, 90%, 95%, 99% - you are monitoring percentiles, right?) we fail the test - performance is just as important as correctness.

<img src="/images/posts/cf-jenkins-pipelines/percentiles.png">

Now, each AWS end-to-end deployment is a Jenkins Pipeline, which is itself kicked off by the full End-to-End testing Pipeline job, while the end-to-end deployment cleanup is a traditional Jenkins 'freestyle project'.

# Summary

Jenkins Pipelines enable code-based definitions of jobs and can be incrementally added to existing installations. And they are way better, once you get used to the DSL. By leveraging Cloudformation and some "templating of templates", you can create production-like deployments for comprehensive integration testing. With these tools, there is really no reason to not be setup an integrated, automated continuous integration tool that automatically promotes changes into production.

[AWS Cloudformation]: https://aws.amazon.com/cloudformation/
[Liquid]: https://shopify.github.io/liquid/