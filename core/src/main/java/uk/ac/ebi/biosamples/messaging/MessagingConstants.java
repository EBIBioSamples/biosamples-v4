/*
* Copyright 2021 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.messaging;

public class MessagingConstants {
  public static final String INDEXING_EXCHANGE = "biosamples.forindexing.solr";
  public static final String INDEXING_QUEUE = "biosamples.tobeindexed.solr";
  public static final String REINDEXING_EXCHANGE = "biosamples.reindex.solr";
  public static final String REINDEXING_QUEUE = "biosamples.reindex.solr";
  public static final String UPLOAD_QUEUE = "biosamples.uploaded.files";
  public static final String UPLOAD_EXCHANGE = "biosamples.uploaded.files.exchange";
}
