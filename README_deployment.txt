http://central.sonatype.org/pages/ossrh-guide.html

The project has been set up to push artifacts to sonatype public staging repository

You will require a sonatype JIRA account 

https://issues.sonatype.org/secure/Signup!default.jspa

You will need update your ~/.m2/settings.xml with you account details :

<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>your-jira-id</username>
      <password>your-jira-pwd</password>
    </server>
  </servers>
</settings>

To publish all the artifacts execute the following :

mvn clean deploy

For more information on configuring maven for sonatype see :

http://central.sonatype.org/pages/apache-maven.html
