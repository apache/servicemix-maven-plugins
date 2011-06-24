/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.docs.confluence;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;

/**
 * Represents a document in Confluence wiki markup
 */
public class ConfluenceConverter {

    final ConfluenceLanguage markupLanguage = new ConfluenceLanguage();

    public ConfluenceConverter() {
    }

    public void convert(Reader reader, Writer writer) throws IOException {
        DocBookDocumentBuilder builder = new DocBookDocumentBuilder(writer);
        MarkupParser parser = new MarkupParser();
        parser.setMarkupLanguage(markupLanguage);
        parser.setBuilder(builder);
        parser.parse(reader);
        writer.flush();
    }

}
