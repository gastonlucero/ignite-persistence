package test.nodes

import java.util.concurrent.TimeUnit
import javax.cache.expiry.CreatedExpiryPolicy

import org.apache.ignite.Ignition
import org.apache.ignite.cache.{CacheMode, CacheWriteSynchronizationMode}
import org.apache.ignite.configuration.{IgniteConfiguration, _}

import scala.util.Random

case class Device(id: String, metadata: String, lat: Double, lon: Double) {
  override def toString = s"Device[id = $id - metadata = $metadata - lat = $lat - lon = $lon}"
}


object TestNativePersistence extends App {

//  val configFactory =ConfigFactory.load()



//  // create a new instance of TCP Discovery SPI// create a new instance of TCP Discovery SPI
//  val spi = new TcpDiscoverySpi
//  // create a new instance of tcp discovery multicast ip finder
//  val tcMp = new TcpDiscoveryMulticastIpFinder
//  tcMp.setAddresses(util.Arrays.asList(s"${InetAddress.getLocalHost}:8000")) // change your IP address here
//  // set the multi cast ip finder for spi
//  println(s"Multicast ip ${tcMp.getMulticastGroup}")
//  println(s"Multicast port ${tcMp.getMulticastPort}")
//  spi.setIpFinder(tcMp)

  // Apache Ignite node configuration.
//  config.setDiscoverySpi(spi)
//  configFactory.getString("persistence.path")

  val NativePersistence = "device_native"
  val PersistencePath = "/tmp/ignite"
  val WalPath = "/tmp/wal"
  val config = new IgniteConfiguration()

  val cacheCfg = new CacheConfiguration(NativePersistence)
  cacheCfg.setBackups(2)
  cacheCfg.setCacheMode(CacheMode.REPLICATED)
  cacheCfg.setWriteSynchronizationMode(CacheWriteSynchronizationMode.FULL_SYNC)
 cacheCfg.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new javax.cache.expiry.Duration(TimeUnit.SECONDS, 30)))
//
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

  cacheCfg.getExpiryPolicyFactory
  def random(min: Int, max: Int): Double = {
    val r = new Random
    min + (max - min) * r.nextDouble
  }

//  for (i <- 1 to 100) {
//   cache.put(i, Device(i.toString, s"metadata $i", random(-90, 90), random(-180, 180)))
//  }

  for (i <- 1 to 100) {
    println(s"Get device ${cache.get(i)}")
  }
//

  cache.remove(1)
//
//  for (i <- 1 to 100) {
//    println(s"Get device ${cache.get(i)}")
//  }


}
