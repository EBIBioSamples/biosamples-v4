/*
* Copyright 2019 EMBL - European Bioinformatics Institute
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
* file except in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the
* License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
* CONDITIONS OF ANY KIND, either express or implied. See the License for the
* specific language governing permissions and limitations under the License.
*/
package uk.ac.ebi.biosamples.legacy.xml.model;

public enum LegacyQueryRegularExpressions implements RegExpProvider {
  DATE_RANGE_FILTER_QUERY {
    @Override
    public String getPattern() {
      final String datePattern = "\\d{4}-\\d{2}-\\d{2}";
      final String encodedAndDecodedSpaces = "(?:\\s|%20)+";
      final String dateRangePattern =
          "\\[(?<from>"
              + datePattern
              + ")"
              + encodedAndDecodedSpaces
              + "TO"
              + encodedAndDecodedSpaces
              + "(?<until>"
              + datePattern
              + ")]";
      final String typeRangePattern = "(?<type>update|release)date:";
      final String dateRangeFinalPattern = "(?:" + typeRangePattern + dateRangePattern + ")";
      //            final String matchesIfAsWholeLine = "^" + dateRangeFinalPattern + "$";
      return dateRangeFinalPattern;
    }
  },

  SAMPLE_ACCESSION_FILTER_QUERY {
    @Override
    public String getPattern() {
      return "(?<accession>SAM[EA|N|E](\\S)*)";
    }
  },

  AND {
    @Override
    public String getPattern() {
      return "(?<and>(?:%20|\\s)*AND(?:%20|\\s)*)?";
    }
  }
}
