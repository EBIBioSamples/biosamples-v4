#Using Etags

Here is an example of how BioSamples ETag can be used to identify samples that have changed.

Get the sample in a normal way:

`curl -H "Accept: application/json" -i http://localhost:8081/biosamples/samples/SAMEA99332211`

This will return an ETag header field, the value of which you can store. e.g.

```
HTTP/1.1 200
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
X-Frame-Options: DENY
X-Application-Context: application
Cache-Control: max-age=60, public
ETag: "08ba14a703c0678cdcb7453f5be41b809"
Content-Type: application/json;charset=UTF-8
Content-Length: 1349
Date: Mon, 13 Aug 2018 08:46:53 GMT
```

In subsequent runs of the import you can now include the ETag in a If-None-Match header field:

`curl -H "Accept: application/json" -H 'If-None-Match: "08ba14a703c0678cdcb7453f5be41b809"' -i http://localhost:8081/biosamples/samples/SAMEA99332211`

If the Sample has not changed the ETag will not have changed and you will get a 304 status back e.g.

```
HTTP/1.1 304
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
X-Frame-Options: DENY
X-Application-Context: application
Cache-Control: max-age=60, public
ETag: "08ba14a703c0678cdcb7453f5be41b809"
Date: Mon, 13 Aug 2018 08:48:34 GMT
```

If there has been a change you will get a full get response back as before but with a different ETag.