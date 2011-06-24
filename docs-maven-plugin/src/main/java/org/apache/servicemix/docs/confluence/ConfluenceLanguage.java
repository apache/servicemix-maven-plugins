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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.mylyn.internal.wikitext.confluence.core.block.AbstractConfluenceDelimitedBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.block.ColorBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.block.ExtendedPreformattedBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.block.ExtendedQuoteBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.block.HeadingBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.block.ListBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.block.QuoteBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.block.TableOfContentsBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.block.TextBoxBlock;
import org.eclipse.mylyn.internal.wikitext.confluence.core.phrase.ConfluenceWrappedPhraseModifier;
import org.eclipse.mylyn.internal.wikitext.confluence.core.phrase.EmphasisPhraseModifier;
import org.eclipse.mylyn.internal.wikitext.confluence.core.phrase.ImagePhraseModifier;
import org.eclipse.mylyn.internal.wikitext.confluence.core.phrase.SimplePhraseModifier;
import org.eclipse.mylyn.internal.wikitext.confluence.core.phrase.SimpleWrappedPhraseModifier;
import org.eclipse.mylyn.internal.wikitext.confluence.core.token.EscapedCharacterReplacementToken;
import org.eclipse.mylyn.wikitext.core.parser.Attributes;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder.BlockType;
import org.eclipse.mylyn.wikitext.core.parser.DocumentBuilder.SpanType;
import org.eclipse.mylyn.wikitext.core.parser.LinkAttributes;
import org.eclipse.mylyn.wikitext.core.parser.markup.Block;
import org.eclipse.mylyn.wikitext.core.parser.markup.ContentState;
import org.eclipse.mylyn.wikitext.core.parser.markup.PatternBasedElement;
import org.eclipse.mylyn.wikitext.core.parser.markup.PatternBasedElementProcessor;
import org.eclipse.mylyn.wikitext.core.parser.markup.token.EntityReferenceReplacementToken;
import org.eclipse.mylyn.wikitext.core.parser.markup.token.ImpliedHyperlinkReplacementToken;
import org.eclipse.mylyn.wikitext.core.parser.markup.token.PatternEntityReferenceReplacementToken;
import org.eclipse.mylyn.wikitext.core.parser.markup.token.PatternLineBreakReplacementToken;
import org.eclipse.mylyn.wikitext.core.parser.markup.token.PatternLiteralReplacementToken;


public class ConfluenceLanguage extends org.eclipse.mylyn.wikitext.confluence.core.ConfluenceLanguage {

    private final List<Block> nestedBlocks = new ArrayList<Block>();

    private final ContentState contentState = new ContentState();

    @Override
    protected void clearLanguageSyntax() {
        super.clearLanguageSyntax();
        nestedBlocks.clear();
    }

    public List<Block> getNestedBlocks() {
        return nestedBlocks;
    }

    @Override
    protected void addStandardBlocks(List<Block> blocks, List<Block> paragraphBreakingBlocks) {
        // IMPORTANT NOTE: Most items below have order dependencies.  DO NOT REORDER ITEMS BELOW!!

        HeadingBlock headingBlock = new HeadingBlock();
        blocks.add(headingBlock);
        paragraphBreakingBlocks.add(headingBlock);
        nestedBlocks.add(headingBlock);
        ConfluenceGlossaryBlock glossaryBlock = new ConfluenceGlossaryBlock();
        blocks.add(glossaryBlock);
        paragraphBreakingBlocks.add(glossaryBlock);
        nestedBlocks.add(glossaryBlock);
        ListBlock listBlock = new ListBlock();
        blocks.add(listBlock);
        paragraphBreakingBlocks.add(listBlock);
        nestedBlocks.add(listBlock);
        blocks.add(new QuoteBlock());
        TableBlock tableBlock = new TableBlock();
        blocks.add(tableBlock);
        paragraphBreakingBlocks.add(tableBlock);
        nestedBlocks.add(tableBlock);
        ExtendedQuoteBlock quoteBlock = new ExtendedQuoteBlock();
        blocks.add(quoteBlock);
        paragraphBreakingBlocks.add(quoteBlock);
        ExtendedPreformattedBlock noformatBlock = new ExtendedPreformattedBlock();
        blocks.add(noformatBlock);
        paragraphBreakingBlocks.add(noformatBlock);

        blocks.add(new TextBoxBlock(BlockType.PANEL, "panel")); //$NON-NLS-1$
        blocks.add(new TextBoxBlock(BlockType.NOTE, "note")); //$NON-NLS-1$
        blocks.add(new TextBoxBlock(BlockType.INFORMATION, "info")); //$NON-NLS-1$
        blocks.add(new TextBoxBlock(BlockType.WARNING, "warning")); //$NON-NLS-1$
        blocks.add(new TextBoxBlock(BlockType.TIP, "tip")); //$NON-NLS-1$
        CodeBlock codeBlock = new CodeBlock();
        blocks.add(codeBlock);
        paragraphBreakingBlocks.add(codeBlock);
        blocks.add(new TableOfContentsBlock());
        ColorBlock colorBlock = new ColorBlock();
        blocks.add(colorBlock);
        paragraphBreakingBlocks.add(colorBlock);
    }

    @Override
    protected void addStandardPhraseModifiers(PatternBasedSyntax phraseModifierSyntax) {
        phraseModifierSyntax.beginGroup("(?:(?<=[\\s\\.,\\\"'?!;:\\)\\(\\[\\]])|^)(?:", 0); //$NON-NLS-1$
        phraseModifierSyntax.add(new HyperlinkPhraseModifier());
        phraseModifierSyntax.add(new SimplePhraseModifier("*", SpanType.STRONG, true)); //$NON-NLS-1$
        phraseModifierSyntax.add(new EmphasisPhraseModifier());
        phraseModifierSyntax.add(new SimplePhraseModifier("??", SpanType.CITATION, true)); //$NON-NLS-1$
        phraseModifierSyntax.add(new SimplePhraseModifier("-", SpanType.DELETED, true)); //$NON-NLS-1$
        phraseModifierSyntax.add(new SimplePhraseModifier("+", SpanType.UNDERLINED, true)); //$NON-NLS-1$
        phraseModifierSyntax.add(new SimplePhraseModifier("^", SpanType.SUPERSCRIPT, false)); //$NON-NLS-1$
        phraseModifierSyntax.add(new SimplePhraseModifier("~", SpanType.SUBSCRIPT, false)); //$NON-NLS-1$
        phraseModifierSyntax.add(new SimpleWrappedPhraseModifier("{{", "}}", DocumentBuilder.SpanType.MONOSPACE, false)); //$NON-NLS-1$ //$NON-NLS-2$
        phraseModifierSyntax.add(new ConfluenceWrappedPhraseModifier("{quote}", DocumentBuilder.SpanType.QUOTE, true)); //$NON-NLS-1$
        phraseModifierSyntax.add(new ImagePhraseModifier());
        phraseModifierSyntax.endGroup(")(?=\\W|$)", 0); //$NON-NLS-1$
    }

    @Override
    protected void addStandardTokens(PatternBasedSyntax tokenSyntax) {
        tokenSyntax.add(new PatternLineBreakReplacementToken("(\\\\\\\\)")); // line break //$NON-NLS-1$
        tokenSyntax.add(new EscapedCharacterReplacementToken()); // ORDER DEPENDENCY must come after line break
        tokenSyntax.add(new EntityReferenceReplacementToken("(tm)", "#8482")); //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new EntityReferenceReplacementToken("(TM)", "#8482")); //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new EntityReferenceReplacementToken("(c)", "#169")); //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new EntityReferenceReplacementToken("(C)", "#169")); //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new EntityReferenceReplacementToken("(r)", "#174")); //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new EntityReferenceReplacementToken("(R)", "#174")); //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new PatternEntityReferenceReplacementToken("(?:(?<=\\w\\s)(---)(?=\\s\\w))", "#8212")); // emdash //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new PatternEntityReferenceReplacementToken("(?:(?<=\\w\\s)(--)(?=\\s\\w))", "#8211")); // endash //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new PatternLiteralReplacementToken("(----)", "<hr/>")); // horizontal rule //$NON-NLS-1$ //$NON-NLS-2$
        tokenSyntax.add(new ImpliedHyperlinkReplacementToken());
        tokenSyntax.add(new AnchorReplacementToken());
    }

    @Override
    protected ContentState createState() {
        return contentState;
    }

    public static class CodeBlock extends AbstractConfluenceDelimitedBlock {

        private String title;

        private String language;

        public CodeBlock() {
            super("code"); //$NON-NLS-1$
        }

        @Override
        protected void beginBlock() {
            if (title != null) {
                Attributes attributes = new Attributes();
                attributes.setTitle(title);
                builder.beginBlock(DocumentBuilder.BlockType.PANEL, attributes);
            }
            Attributes attributes = new Attributes();
            Attributes preAttributes = new Attributes();
            if (language != null) {
                attributes.setLanguage(language); //$NON-NLS-1$
            }
//		builder.beginBlock(DocumentBuilder.BlockType.PREFORMATTED, preAttributes);
            builder.beginBlock(DocumentBuilder.BlockType.CODE, attributes);
            builder.charactersUnescaped("<![CDATA[");
        }

        @Override
        protected void handleBlockContent(String content) {
            builder.charactersUnescaped(content);
            builder.charactersUnescaped("\n"); //$NON-NLS-1$
        }

        @Override
        protected void endBlock() {
            builder.charactersUnescaped("]]>");
            if (title != null) {
                builder.endBlock(); // panel
            }
            builder.endBlock(); // code
//		builder.endBlock(); // pre
        }

        @Override
        protected void resetState() {
            super.resetState();
            title = null;
        }

        @Override
        protected void setOption(String key, String value) {
            if (key.equals("title")) { //$NON-NLS-1$
                title = value;
            } else if (key.equals("lang")) {
                language = value;
            }
        }

        @Override
        protected void setOption(String option) {
            language = option.toLowerCase();
        }
    }

    public static class ConfluenceGlossaryBlock extends Block {

        static final Pattern startPattern = Pattern.compile("\\s*-\\s*+(.*?)\\s*+::\\s*+(.*?)\\s*+"); //$NON-NLS-1$

        private Matcher matcher;
        private int blockLineCount = 0;

        @Override
        public int processLineContent(String line, int offset) {
            if (blockLineCount == 0) {
                builder.beginBlock(BlockType.DEFINITION_LIST, new Attributes());
            } else {
                matcher = startPattern.matcher(line);
                if (!matcher.matches()) {
                    setClosed(true);
                    return 0;
                }
            }
            ++blockLineCount;
            String key = matcher.group(1);
            String val = matcher.group(2);
            builder.beginBlock(BlockType.DEFINITION_TERM, new Attributes());
            markupLanguage.emitMarkupLine(getParser(), state, key, 0);
            builder.endBlock();
            builder.beginBlock(BlockType.DEFINITION_ITEM, new Attributes());
            markupLanguage.emitMarkupLine(getParser(), state, val, 0);
            builder.endBlock();
            return -1;
        }

        @Override
        public boolean canStart(String line, int lineOffset) {
            if (lineOffset == 0 && !markupLanguage.isFilterGenerativeContents()) {
                matcher = startPattern.matcher(line);
                return matcher.matches();
            } else {
                matcher = null;
                return false;
            }
        }

        @Override
        public void setClosed(boolean closed) {
            if (closed && !isClosed()) {
                builder.endBlock();
            }
            super.setClosed(closed);
        }
    }


    public static class TableBlock extends Block {

        static final Pattern startPattern = Pattern.compile("(\\|(.*)?(\\|\\s*$))"); //$NON-NLS-1$

        static final Pattern TABLE_ROW_PATTERN = Pattern.compile("\\|(\\|)?" + "((?:(?:[^\\|\\[]*)(?:\\[[^\\]]*\\])?)*)" //$NON-NLS-1$ //$NON-NLS-2$
                + "(\\|\\|?\\s*$)?"); //$NON-NLS-1$

        private int blockLineCount = 0;

        private Matcher matcher;

        public TableBlock() {
        }

        @Override
        public int processLineContent(String line, int offset) {
            if (blockLineCount == 0) {
                Attributes attributes = new Attributes();
                builder.beginBlock(BlockType.TABLE, attributes);
            } else if (markupLanguage.isEmptyLine(line)) {
                setClosed(true);
                return 0;
            }
            ++blockLineCount;

            if (offset == line.length()) {
                return -1;
            }

            String textileLine = offset == 0 ? line : line.substring(offset);
            Matcher rowMatcher = TABLE_ROW_PATTERN.matcher(textileLine);
            if (!rowMatcher.find()) {
                setClosed(true);
                return 0;
            }

            builder.beginBlock(BlockType.TABLE_ROW, new Attributes());

            do {
                int start = rowMatcher.start();
                if (start == textileLine.length() - 1) {
                    break;
                }

                String headerIndicator = rowMatcher.group(1);
                String text = rowMatcher.group(2).trim(); // MODIFIED: added trim()
                int lineOffset = offset + rowMatcher.start(2);

                boolean header = headerIndicator != null && "|".equals(headerIndicator); //$NON-NLS-1$

                Attributes attributes = new Attributes();
                builder.beginBlock(header ? BlockType.TABLE_CELL_HEADER : BlockType.TABLE_CELL_NORMAL, attributes);

                markupLanguage.emitMarkupLine(getParser(), state, lineOffset, text, 0);

                builder.endBlock(); // table cell
            } while (rowMatcher.find());

            builder.endBlock(); // table row

            return -1;
        }

        @Override
        public boolean canStart(String line, int lineOffset) {
            blockLineCount = 0;
            if (lineOffset == 0) {
                matcher = startPattern.matcher(line);
                return matcher.matches();
            } else {
                matcher = null;
                return false;
            }
        }

        @Override
        public void setClosed(boolean closed) {
            if (closed && !isClosed()) {
                builder.endBlock();
            }
            super.setClosed(closed);
        }

    }

    public static class HyperlinkPhraseModifier extends PatternBasedElement {

        @Override
        protected String getPattern(int groupOffset) {
            return "\\[(?:\\s*([^\\]\\|]+)\\|)?([^\\]]+)\\]"; //$NON-NLS-1$
        }

        @Override
        protected int getPatternGroupCount() {
            return 2;
        }

        @Override
        protected PatternBasedElementProcessor newProcessor() {
            return new HyperlinkPhraseModifierProcessor();
        }

        private static class HyperlinkPhraseModifierProcessor extends PatternBasedElementProcessor {
            @Override
            public void emit() {
                String text = group(1);
                String linkComposite = group(2);
                String[] parts = linkComposite.split("\\s*\\|\\s*"); //$NON-NLS-1$
                if (parts.length == 0) {
                    // can happen if linkComposite is ' |', see bug 290434
                } else {
                    if (text != null) {
                        text = text.trim();
                    }
                    String href = parts[0];
                    if (href != null) {
                        href = href.trim();
                    }
                    if (href.charAt(0) == '#') {

                    }
                    String tip = parts.length > 1 ? parts[1] : null;
                    if (tip != null) {
                        tip = tip.trim();
                    }
                    if (text == null || text.length() == 0) {
                        text = href;
                        if (text.length() > 0 && text.charAt(0) == '#') {
                            text = text.substring(1);
                        }
                        if (href.charAt(0) == '#') {
                            href = "#" + state.getIdGenerator().getGenerationStrategy().generateId(href.substring(1));
                        }
                        Attributes attributes = new LinkAttributes();
                        attributes.setTitle(tip);
                        getBuilder().link(attributes, href, text);
                    } else {
                        if (href.charAt(0) == '#') {
                            href = "#" + state.getIdGenerator().getGenerationStrategy().generateId(href.substring(1));
                        }
                        LinkAttributes attributes = new LinkAttributes();
                        attributes.setTitle(tip);
                        attributes.setHref(href);
                        getBuilder().beginSpan(SpanType.LINK, attributes);

                        getMarkupLanguage().emitMarkupLine(parser, state, start(1), text, 0);

                        getBuilder().endSpan();
                    }
                }
            }
        }
    }

    public static class AnchorReplacementToken extends PatternBasedElement {

        @Override
        protected String getPattern(int groupOffset) {
            return "\\{anchor:([^\\}]+)\\}"; //$NON-NLS-1$
        }

        @Override
        protected int getPatternGroupCount() {
            return 1;
        }

        @Override
        protected PatternBasedElementProcessor newProcessor() {
            return new AnchorReplacementTokenProcessor();
        }

        private static class AnchorReplacementTokenProcessor extends PatternBasedElementProcessor {
            @Override
            public void emit() {
                String name = group(1);
                name = state.getIdGenerator().getGenerationStrategy().generateId(name);
                Attributes attributes = new Attributes();
                attributes.setId(name);
                getBuilder().beginSpan(SpanType.SPAN, attributes);
                getBuilder().endSpan();
            }

        }

    }
 }
