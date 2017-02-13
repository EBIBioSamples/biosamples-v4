Deployment
==========

The project has been set up to push artifacts to Sonatype public staging repository.
See http://central.sonatype.org/pages/ossrh-guide.html for details.

You will require a Sonatype JIRA account and can sign up at 
https://issues.sonatype.org/secure/Signup!default.jspa

Once you've registered, you will need update your ~/.m2/settings.xml with your
account details :

<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>YOUR-SONATYPE-JIRA-ID</username>
      <password>YOUR-SONATYPE-JIRA-PASSWORD</password>
    </server>
  </servers>
</settings>

To publish all the artifacts execute the following :

mvn deploy

For more information on configuring Maven for Sonatype see :

http://central.sonatype.org/pages/apache-maven.html
