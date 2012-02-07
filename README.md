# Stanbol Temis

This project is an OSGi bundle extension for the [Apache Stanbol (Incubating)]
[1] server to make Stanbol delegate the analysis of a piece of text to an
instance of the [Temis Luxid Annotation Factory] [2] web service.

Temis is typically used to extract occurrences of named entities in the body of
the text, for instance: places, organizations and people.

[1]: http://incubator.apache.org/stanbol
[2]: http://www.temis.com

## License

This connector is realeased under the terms of the ASL 2.0.

## Building and deploying

To build the jar and run the tests:

    mvn install

The bundle jar can then be found in the `target/` subfolder.

To build and deploy on a live Stanbol instance running on
`http://localhost:8080` just do:

    mvn install -o -DskipTests -PinstallBundle \
        -Dsling.url=http://localhost:8080/system/console

Alternatively you can build the `stanbol-temis` launcher jar in
in the `launcher` subfolder and start it with
`java -jar stanbol-temis-*.jar`.

This launcher is configured to use a pre-configured dbpedia index
available at:

    http://dev.iks-project.eu/downloads/stanbol-indices/dbpedia-3.7/dbpedia.solrindex.zip

Just download this zip archive and put it under the `sling/datafiles/`
folder of your instance. Stanbol will decompress it under `sling/entityhub/`
automatically when it needs it.

Then go to the OSGi system console to set the URI and credentials
of your Luxid instance. Typically the URL looks like:

    http://localhost:8080/system/console/configMgr/org.nuxeo.stanbol.temis.TemisEnhancementEngine

The URL of the Luxid webservice typically has the following form:

    http://example.com/LuxidWS/services/Annotation?wsdl


## Running the engine from a REST client

You can then analyse your text content as usual using the Stanbol rest API:

    curl -X POST -H "Accept: application/json" -H "Content-type: text/plain" \
        --data "John Smith works at Smith Consulting in London, UK." \
        http://localhost:8080/engines/

The resulting annotations are expressed using the Stanbol RDF vocabulary.


## From a Nuxeo Document Management server

Install Nuxeo DM 5.4.0.1+ and the [Semantic Entities add-on] [3] from
the marketplace. Don't forget to [configure] [4] the add-on to point to
your own Stanbol instance.

[3]: https://connect.nuxeo.com/nuxeo/site/marketplace/package/semantic-entities-1.0.0
[4]: https://doc.nuxeo.com/display/NXDOC/Semantic+Entities+Installation+and+Configuration


## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software
platform for enterprise content management] [5] and packaged applications
for [document management] [6], [digital asset management] [7] and
[case management] [8]. Designed by developers for developers, the Nuxeo
platform offers a modern architecture, a powerful plug-in model and
extensive packaging capabilities for building content applications.

[5]: http://www.nuxeo.com/en/products/ep
[6]: http://www.nuxeo.com/en/products/document-management
[7]: http://www.nuxeo.com/en/products/dam
[8]: http://www.nuxeo.com/en/products/case-management

More information on: <http://www.nuxeo.com/>
