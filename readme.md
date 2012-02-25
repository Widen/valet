Valet is a simple Java API to interact AWS's [Route53](http://aws.amazon.com/route53/) DNS service.

The [AWS Java SDK](http://aws.amazon.com/releasenotes/Java/4090905529949639) added Route53 in December 2011.
This simple binding predates the native SDK implementation and includes several tools that may be useful.

View [ValetExample](/Widen/valet/blob/master/src/main/java/com/widen/examples/ValetExample.java) for typical usage of Valet API.

Includes [automation classes](/Widen/valet/tree/master/src/main/java/com/widen/valet/importer) to import/one-way-sync existing Windows DNS server files.

To run the example [ZoneSummary](/Widen/valet/blob/master/src/main/java/com/widen/examples/ZoneSummary.java) application using [Gradle](http://www.gradle.org/)

    gradle zoneSummary -Paws-access-key=MY_AWS_ACCESS_KEY -Paws-secret-key=MY_AWS_SECRET_KEY

Available via Maven:

      server: http://widen.artifactoryonline.com/widen/libs-widen-public
       group: widen
    artifact: valet
     version: 0.3

Or browse the repo directly at [https://widen.artifactoryonline.com/widen/libs-widen-public](https://widen.artifactoryonline.com/widen/libs-widen-public/widen/valet/)

Change Log:

	0.3 - Remove extraneous system error debug message

    0.2 - Weighted Round Robin support
    	  Alias Resource Record support
          Delete zone implemented
          HttpClient instance can be supplied to Route53Driver to support custom proxy server configuration

    0.1 - Initial version

Contact Uriah Carpenter (uriah at widen.com) with questions.

Licensed under Apache, Version 2.0.