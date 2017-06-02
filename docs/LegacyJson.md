# Legacy Json

## Use resource support to easy the process of building the API
Looking at the OpenCredo hateoas example available on github 
[here](https://github.com/opencredo/spring-hateoas-sample/tree/master/src/main/java/com/opencredo/demo/hateoas) 
I got the inspiration to sue the ResourceSupport class to build the index of the api.
This way I can assemble the links and content in an easy way

## Add Spring-data-rest to the dependencies
Without spring data rest, the serialization of the content is not
made properly following HATEOAS specification. Adding the depenency solved the problem.
(e.g links are serialized into "_links" with spring data rest, "links" without)