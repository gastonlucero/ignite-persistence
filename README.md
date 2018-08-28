<h4> Build you own Apache Ignite persistence with Scala

Let suppose that we are working on a real time iot application, designed to receive events from devices such as
temperature devices, gps, or events from raspberry sensors.
We assume that an event has an id, the metadata event ,and lat-long coordinates for geospatial queries (shortest route maybe)
Generally you need to work with a backoffice application through which the users can create devices based on an
unique id, like the imei, or gpio , etc. 
This organizational data is static, but the sensors could send events every minute, or less in gps case, we
 repeatedly come up against the problem of enrich the dynamic data with the static data, with the plus that the 
 devices usually are waiting the ack message.
How can Apache Ignite help us with this situation? static data and dynamic data in multiple microservices for example?
Well ,besides using the cache benefits of Ignite as we have seen in previous post, replication, repartition, queries,
Ignite has persistence for the cached data only? let`s take a deeper look at this point 

<h5> Persistence

There are a variety of databases that are suitable for persist data, but how many in-memory platforms provide cache and 
store without boilerplate code?

A simple example to show how it works

Note: By default, Ignite nodes consume up to 20% of the RAM available locally