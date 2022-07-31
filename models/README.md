# README
These are all the models that will be used within the system.

Note that currently they are all one module, so that module has the
dependencies of all of them, and therefore anything that uses any of
the models has to pull all of the transitive dependencies.

## JsonLD Module
Contains all the classes modeling the ld+json world for the BioSchema project

## Sitemap Module
All classes used to create the sitemap are in this module and reused in the
**webpps-core** module
