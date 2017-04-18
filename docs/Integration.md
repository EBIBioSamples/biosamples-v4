# Integration

## Application structure
The integration module is split in two phases. During phase 1 all input to BioSamples should be made. 
During phase 2 instead, should be possible to query BioSamples back. That means that between phase 1 
and phase 2 the queues (RabbitMQ) are populated and processed to integrate changes.

## Phase enumeration
- In order to make it easy to extend and understand the code in the runners, I've 
build a `Phase` enum class to handle different phases and reference them in a meaningful way