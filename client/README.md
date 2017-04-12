# Client

The client module represent the programmatic interface that users should use 
to talk query the BioSamples database.

The Client talks through the `webapps-core` module. That means when
the `webapps-core` is the actual source of truth and if we need to change
anything, we just need to change it there.