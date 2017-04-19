# Integration

## Application structure
The integration module is split in two phases. During phase 1 all input to BioSamples should be made. 
During phase 2 instead, should be possible to query BioSamples back. That means that between phase 1 
and phase 2 the queues (RabbitMQ) are populated and processed to integrate changes.

## Phase enumeration
- In order to make it easy to extend and understand the code in the runners, I've 
build a `Phase` enum class to handle different phases and reference them in a meaningful way

## Order and profile for runners modularity
I changed the runner to use more Spring annotations to work. First the Ordered interface
has been substituted by the `@Order` annotation. This way it's easier to spot immediately
the order of the runners.
Secondly, I've activated profiles for each runner. Since the integration application is
a integration test class, defining different profiles we can decide to switch on/off 
each runner based on the profile we want to use. This is really useful for example if 
we want to test a single module excluding all the others. Add the "test" profile to the
module and pass the option `--spring.profiles.active=test` to the application, we can 
run just the desired modules.
Since I've activated the profiles, it's necessary for each runner to have at least the
default profile in order to run when no specific profiles are added. This means add the annotation
`@Profile({"default"})` or `@Profile("default")`
