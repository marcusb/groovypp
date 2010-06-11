@Typed package sd

import groovy.util.concurrent.CallLaterExecutors

def shapeFile = new File("shapes.txt")

//BEGIN Bootstrap
//create shapes.txt for demo purposes
//def bootstrapData = """shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence,shape_dist_traveled
//509029, 39.65624,-104.99946,1,
//509029, 39.65658,-104.99931,2,
//509029, 39.65673,-104.99921,3,
//509029, 39.65673,-104.99908,4,
//509029, 39.65674,-104.99779,5,
//509029, 39.65674,-104.99771,6,
//509029, 39.65676,-104.99700,7,
//509029, 39.65675,-104.99641,8,
//509029, 39.65675,-104.99585,9,
//509029, 39.65674,-104.99463,10,
//509029, 39.65674,-104.99463,11,
//509029, 39.65672,-104.99356,12,
//509029, 39.65645,-104.99354,13,
//509029, 39.65577,-104.99354,14,
//"""
//shapeFile.text = bootstrapData
//return
//END Bootstrap

//get headers from first line
def lineIterator = shapeFile.newReader().iterator()
def headers = lineIterator.next().split(",") as List
assert headers.size() == 5

def startTime = new Date()
println "Start time: ${startTime}"
//TODO: insert GPars magic here

def pool = CallLaterExecutors.newFixedThreadPool(10)
// map line iterator to concurrent iterator
// thread pool of 10 threads, not preserving order, maximum 10 tasks simultaniously
lineIterator.mapConcurrently(pool, false, 10) {line->
  //split csv
  //cast to List to avoid ArrayIndexOutOfBoundsException on last missing element
  def values = line.split(",") as List

  //create hashmap
  def map = [:]
  for (i in 0..<headers.size())
      map[headers[i]] = values[i] ?: ""

  //HTTP PUT into CouchDB
  // curl -vX GET http://127.0.0.1:5984/_uuids
  // curl -vX PUT http://127.0.0.1:5984/shapes/uuid123 -d '{"shape_id":"509029", "shape_pt_lat":"39.65624", ...}'
  // we'll just use a println as a proxy for the HTTP traffic for now
  println map
}.each { /* as mapConcurrently returns iterator we need each{} */ }
pool.shutdown()

println "Start time: ${startTime}"
println "End time: ${new Date()}"

//Start time: Fri Jun 11 10:53:13 MDT 2010
//End time: Fri Jun 11 10:53:46 MDT 2010

