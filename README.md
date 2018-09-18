<h4> Build you own Apache Ignite persistence connector

In previous post about Apache Ignite, we learn how to setup and create either a simple cache or sql cache, and share the 
cached data between different nodes.
In this post we will go a little bit deeper, because, if our app crash, cached data disappears,how ignite help us to avoid
this result?

Let suppose that we are working on a real time iot application, designed to receive events from devices such as
temperature devices, gps, or events from raspberry sensors.
We assume that an event has an id, the metadata event ,and lat-long coordinates for geospatial queries (shortest route maybe),
and exists a backoffice application through which users create metadata for devices based on an
unique id (the same as the received events), like the imei, or gpio , etc. 
This organizational data is static, but the sensors could send events every minute, or less in gps case, we
 repeatedly come up against the problem of enrich the dynamic data with the static data, with the plus that the 
 devices usually are waiting the ack message.
How can Apache Ignite help us with this situation? static data and dynamic data in multiple microservices for example?
Well ,besides using the cache benefits of Ignite as we have seen in previous post, replication, repartition, queries,
Ignite has persistence for the cached data only? let`s take a deeper look at this point 

######Persistence

There are a variety of databases that are suitable for persist data, but how many in-memory platforms provide cache and 
store without boilerplate code?

A simple example to show how it works (We will use Postgres has database, and Scala)

To get started, add the following dependencies in your build.sbt file:

    libraryDependencies ++= Seq(
       "org.apache.ignite" % "ignite-core" % "2.6.0",
       "org.postgresql" % "postgresql" % "42.2.4"
    )

Then configure Ignite cache as usual, adding persistence properties
 
    val NativePersistence = "device_native"
    
Where data will be stored
    
    val PersistencePath = "/tmp/ignite"
    
Where write-ahead log will be stored
    
    val WalPath = "/tmp/wal"
    
    val config = new IgniteConfiguration()

> The purpose of the WAL is to provide a recovery mechanism for scenarios where a single node or the whole cluster goes down
      
In this section , we configure cache backup nodes, cache mode, and expiration policy for the data,for an overview
check Partition and Replication on previous post 
      
    val cacheCfg = new CacheConfiguration(NativePersistence)
    cacheCfg.setBackups(1)
    cacheCfg.setCacheMode(CacheMode.REPLICATED)
    cacheCfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC)
    cacheCfg.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new javax.cache.expiry.Duration(TimeUnit.SECONDS, 30)))
  
Here, we say to Ignite, that we want to persist the cache , by enabling persistence
  
    val storageCfg = new DataStorageConfiguration()
    storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true)
    storageCfg.setStoragePath(PersistencePath)
    storageCfg.setWalPath(WalPath)
    storageCfg.setWalArchivePath(WalPath)
    config.setDataStorageConfiguration(storageCfg)
    config.setCacheConfiguration(cacheCfg)

     val ignition = Ignition.start(config)
     ignition.cluster().active(true)

     val cache = ignition.getOrCreateCache[Int, Device](NativePersistence)

     def random(min: Int, max: Int): Double = {
       val r = new Random
       min + (max - min) * r.nextDouble
     }

     for (i <- 1 to 100) {
       cache.put(i, Device(i.toString, s"metadata $i", random(-90, 90), random(-180, 180)))
     }
  
 
And that's all! , lets run the app to show how it works

    [18:19:14] Data Regions Configured:
    [18:19:14]   ^-- default [initSize=256.0 MiB, maxSize=1.6 GiB, persistenceEnabled=true]

And navigating to /tmp, you will see <b>/tmp/ignite</b> and <b>/tmp/wal</b> directories. 
To be sure that it works, simply, start another instance of the app changing this line

    cache.put(i, Device(i.toString, s"metadata $i", random(-90, 90), random(-180, 180)))

for this one

    println(s"Get device ${cache.get(i)}")
    
then the output look like this

    [18:54:25]   ^-- default [initSize=256.0 MiB, maxSize=1.6 GiB, persistenceEnabled=true]
    Get device Device[id = 1 - metadata = metadata 1 - lat = -77.53427731362423 - lon = 29.076159947913908}
    Get device Device[id = 2 - metadata = metadata 2 - lat = -35.7678515281638 - lon = -13.232386332299711}
    Get device Device[id = 3 - metadata = metadata 3 - lat = 11.884412857887412 - lon = 95.16134531018974}

wait expiry policy configured time , and try again to see the results.

######Baseline Topology
If Ignite persistence is enabled, Ignite enforces the baseline topology concept which represents a set of server nodes 
in the cluster that will persist data on disk. 
Usually, when the cluster is started for the first time with Ignite persistence on, the cluster will be considered inactive 
disallowing any CRUD operations, you need to active the cluster:

    ignition.cluster().active(true)

######Expiry Policies
  - In-Memory Mode (data is stored solely in RAM): expired entries are purged from RAM completely.

  - Memory + Ignite persistence: expired entries are removed from both memory and disk tiers.

  - Memory + 3rd party persistence: expired entries are removed from the memory tier only (Ignite) and left untouched in 
  the 3​rd party persistence (RDBMS, NoSQL, and other databases)

![Policy](expiryPolicy.png)

> Note: By default, Ignite nodes consume up to 20% of the RAM available locally

Great, we have our persistence data in a specific path, in each node, so if a device send an event we will enrich
the message with static metadata, but ...

Above I mentioned that the user can persist devices metadata in a database, if this is the case, then our native persistence
has been "pre loaded" for database, and for every update,application must update the database and refresh the 
associated value in the cache.

If cache depends always on database, there is any possibility to associate database actions to cache??
Maybe Ignite has a way to put/load data into/from cache through database???  (Reminder : Postgres is the database)

######3rd Party Persistence

    import javax.cache.configuration.FactoryBuilder
    
    val JdbcPersistence = "device_ignite_table"   
    val cacheCfg = new CacheConfiguration[String, Device](JdbcPersistence)
    cacheCfg.setBackups(1)
    cacheCfg.setCacheMode(CacheMode.REPLICATED)
    
    cacheCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(classOf[CacheJdbcStore]))
    cacheCfg.setReadThrough(true)
    cacheCfg.setWriteThrough(true)  
    config.setCacheConfiguration(cacheCfg)
    
    val ignition = Ignition.start(config)
    val jdbcCache = ignition.getOrCreateCache[String, Device](JdbcPersistence)
   
CacheStoreFactory is our 3rd party persistence layer, from which, how access/read data form database to/from Ignite is 
managed

    class CacheJdbcStore extends CacheStoreAdapter[String, User] ...
    
If you want to define your cache store adapter, only extends from CacheStoreAdapter[K,V] class, that provides
implementations for commons methods, such as 

- Load all
- Write all
- Delete all

> Ignite provides `org.apache.ignite.cache.store.CacheStore` interface which extends both, `CacheLoader` and `CacheWrite

![CacheStore](https://files.readme.io/2b3807b-in_memory_data.png)

In our example, we will use mostly, two of this methods, **write** and **load**

Write method , related to `cacheCfg.setWriteThrough(true)`, when true means that put a value into cache, under the hood,
calls write method, when `cacheCfg.setWriteThrough(false)`, write it is never called.

     override def write(entry: Cache.Entry[_ <: String, _ <: User]): Unit = {       
        val ps = connection.prepareStatement("INSERT INTO users_table (id,name) VALUES (?,?)")
        ps.setString(1, entry.getKey)
        ps.setString(2, entry.getValue.name)
        ps.executeUpdate()     
      }

The same with read, `cacheCfg.setReadThrough(true)`, when true, if the values is not in cache then it will be find in database

      override def load(key: String): User = {       
        val ps = connection.prepareStatement(s"SELECT * FROM users_table where id = '$key'")
        val rs = ps.executeQuery()
        if (rs.next())
          User(rs.getString("id"), rs.getString("name"))
        else
          null
      } 

> In both cases table name is the same as cache name

If we put some data in `jdbcCache'

for (i <- 1 to 10) {
    jdbcCache.put(i.toString, Device(i.toString, s"metadata $i", random(-90, 90), random(-180, 180)))
  }

 
With this approach, our cache could be always updated!! and depends the case, read or write from database is configurable.
Besides this methods, cacheStore provides, `delete` and `loadCache. 
Imagine, you can use postgres to save your app data, and maybe have a read only cache, for our dashboard view, as in the 
example, or better a write only cache to put data in cache+postgres and read from read only cache.

Bonus track : CacheStoreAdapter using Slick. 
> Slick is a Functional Relational Mapping (FRM) library that makes it easy to work with relational databases

I left the load method subject to free Future[_] interpretation  ;)

######Conclusion
Ignite give us this powerful tool, to maintain our, data, in memory or  memory+native or memory+nosql or memory+jdbc, it is
very flexible and can be adapted to our architectures.
It is possible to use this in a CQRS model?
 

######Related Links

[Source Code](https://github.com/gastonlucero/ignite-persistence)

[Backups](https://apacheignite.readme.io/docs/primary-and-backup-copies)

[BaseLine Topology](https://apacheignite.readme.io/docs/baseline-topology)

[Expiry Policies](https://apacheignite.readme.io/docs/expiry-policies)

[3rd Party Persistence]()https://apacheignite.readme.io/docs/3rd-party-store)

[Slick](https://slick.lightbend.com/doc/3.2.3/introduction.html)


Build docker image 

    docker build . -t ignite-persistence-native

Run image

    docker run  -p 8080:8000 <name>

