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
package uk.ac.ebi.biosamples.solr.repo;

import java.util.ArrayList;
import java.util.Collection;

public class CursorArrayList<T> extends ArrayList<T> {
  private static final long serialVersionUID = -1828085550269612339L;
  private final String nextCursorMark;

  public CursorArrayList(String nextCursorMark) {
    super();
    this.nextCursorMark = nextCursorMark;
  }

  public CursorArrayList(Collection<? extends T> c, String nextCursorMark) {
    super(c);
    this.nextCursorMark = nextCursorMark;
  }

  public CursorArrayList(int initialCapacity, String nextCursorMark) {
    super(initialCapacity);
    this.nextCursorMark = nextCursorMark;
  }

  public String getNextCursorMark() {
    return nextCursorMark;
  }
}
