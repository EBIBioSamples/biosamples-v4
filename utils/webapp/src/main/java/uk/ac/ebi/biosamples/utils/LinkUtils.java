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
package uk.ac.ebi.biosamples.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Link;
import org.springframework.web.util.UriUtils;

public class LinkUtils {

  private static Logger log = LoggerFactory.getLogger(LinkUtils.class);

  public static String decodeText(String text) {
    if (text != null) {
      try {
        // URLDecoder doesn't work right...
        // text = URLDecoder.decode(text, "UTF-8");
        text = UriUtils.decode(text, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return text;
  }

  public static String[] decodeTexts(String[] texts) {
    if (texts != null) {
      for (int i = 0; i < texts.length; i++) {
        try {
          // URLDecoder doesn't work right...
          // filter[i] = URLDecoder.decode(filter[i], "UTF-8");
          texts[i] = UriUtils.decode(texts[i], "UTF-8");
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return texts;
  }

  public static Optional<List<String>> decodeTextsToArray(String[] texts) {
    if (texts == null) {
      return Optional.empty();
    } else {
      List<String> decoded = new ArrayList<>(texts.length);
      for (int i = 0; i < texts.length; i++) {
        try {
          // URLDecoder doesn't work right...
          // filter[i] = URLDecoder.decode(filter[i], "UTF-8");
          decoded.add(UriUtils.decode(texts[i], "UTF-8"));
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
      return Optional.of(decoded);
    }
  }

  public static Link cleanLink(Link link) {
    // expand template to nothing
    link = link.expand(Collections.emptyMap());
    // this won't handle encodings correctly, so need to manually fix that
    link = new Link(decodeText(decodeText(link.getHref())), link.getRel());

    return link;
  }
}
