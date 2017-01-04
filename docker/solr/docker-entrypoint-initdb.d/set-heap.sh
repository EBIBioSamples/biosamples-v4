#!/bin/bash
set -e



cp /opt/solr/bin/solr.in.sh /opt/solr/bin/solr.in.sh.orig

cat /opt/solr/bin/solr.in.sh.orig

cat /opt/solr/bin/solr.in.sh

sed -e 's/#SOLR_HEAP=".*"/SOLR_HEAP="2048m"/' </opt/solr/bin/solr.in.sh.orig >/opt/solr/bin/solr.in.sh
grep '^SOLR_HEAP=' /opt/solr/bin/solr.in.sh
