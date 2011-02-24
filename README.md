# Stanbol Temis

This project is an OSGi bundle extension for the [Apache Stanbol (Incubating)]
[1] server to make Stanbol delegate the analysis of a piece of text to an
instance of the [Temis Luxid Annotation Factory] [2] web service.

[1] http://incubator.apache.org/stanbol
[2] http://www.temis.com

## Building and deploying

To build the jar and run the tests:

    mvn install

The bundle jar can then be found in the `target/` subfolder.

To build and deploy on a live Stanbol instance running on
`http://localhost:8080` just do:

    mvn install -o -DskipTests -PinstallBundle \
        -Dsling.url=http://localhost:8080/system/console

Then go to the OSGi system console to set the URI and credentials of your Luxid
instance:

  http://localhost:8080/system/console/configMgr/org.nuxeo.stanbol.temis.LuxidEnhancementEngine


## Running the engine from a REST client

You can then analyse your text content as usual using the Stanbol rest API:

    curl -X POST -H "Accept: application/json" -H "Content-type: text/plain" \
        --data "John Smith works at Smith Consulting in London, UK." \
        http://localhost:8080/engines/

The resuling annotations are expressed using the Stanbol RDF voabulary.


## From a Nuxeo Document Management server

Install Nuxeo DM 5.4.0.1+ and the Semantic Entities add-on from the marketplace:

  https://connect.nuxeo.com/nuxeo/site/marketplace/package/semantic-entities-1.0.0

Don't forget to configure the add-on to point to your own Stanbol instance:

  https://doc.nuxeo.com/display/NXDOC/Semantic+Entities+Installation+and+Configuration
