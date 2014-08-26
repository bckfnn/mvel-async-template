/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.bckfnn.mvel;

import io.github.bckfnn.mvel.template.CodeNode;
import io.github.bckfnn.mvel.template.CommentNode;
import io.github.bckfnn.mvel.template.DeclareNode;
import io.github.bckfnn.mvel.template.ElseNode;
import io.github.bckfnn.mvel.template.EndNode;
import io.github.bckfnn.mvel.template.ExprNode;
import io.github.bckfnn.mvel.template.ForEachNode;
import io.github.bckfnn.mvel.template.IfNode;
import io.github.bckfnn.mvel.template.IncludeNamedNode;
import io.github.bckfnn.mvel.template.IncludeNode;
import io.github.bckfnn.mvel.template.Node;
import io.github.bckfnn.mvel.template.io.TemplateLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.CompileException;

/**
 * The template compiler. Used to compile an MVEL template and return an Template instance.
 */
public class TemplateCompiler {
    private TemplateLoader templateLoader;

    private Map<String, Class<? extends Node>> commands = new HashMap<String, Class<? extends Node>>();

    /**
     * Construct a template compiler that uses the template loader.
     * @param templateLoader the template loader to use.
     */
    public TemplateCompiler(TemplateLoader templateLoader) {
        this.templateLoader = templateLoader;

        commands.put("foreach", ForEachNode.class);
        commands.put("end", EndNode.class);
        commands.put("if", IfNode.class);
        commands.put("else", ElseNode.class);
        commands.put("code", CodeNode.class);
        commands.put("comment", CommentNode.class);
        commands.put("declare", DeclareNode.class);
        commands.put("includeNamed", IncludeNamedNode.class);
        commands.put("include", IncludeNode.class);
    }

    /**
     * Add a user ORB.
     * @param orbName name of the orb.
     * @param nodeClass class of the implementing node.
     */
    public void addCommand(String orbName, Class<? extends Node> nodeClass) {
        commands.put(orbName, nodeClass);
    }

    /**
     * @return the template loader.
     */
    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    /**
     * Load and compile the specified resources. The template loader is used to loaded the resource content.
     * @param location the location of the release.
     * @return a compiled template.
     * @throws IOException when errors occurs.
     */
    public Template compileResource(String location) throws IOException {
        return compile(templateLoader.sourceAt(location).content().toCharArray());
    }

    /**
     * Compile the specified template.
     * @param content the template
     * @return the compile Template.
     */
    public Template compile(char[] content) {
        //int line = 1;
        Tokenizer t = new Tokenizer(content);
        int textStart = 0;
        TemplateContext context = new TemplateContext(content);

        while (t.hasMore()) {
            switch (t.next()) {
            case '\n':
                //line++;
                break;
            case '@':
                switch (t.peek()) {
                case '{':
                    context.addText(textStart, t.pos-1);
                    Tokenizer.Token expr = t.findMatching('{');
                    context.add(new ExprNode(), expr.start + 1, expr.len - 2);
                    textStart = expr.start + expr.len;
                    continue;
                case '(':
                    context.addText(textStart, t.pos-1);
                    expr = t.findMatching('(');
                    context.add(new ExprNode(), expr.start + 1, expr.len - 2);
                    textStart = expr.start + expr.len;
                    continue;
                case '@':
                    context.addText(textStart, t.pos);
                    t.next();
                    textStart = t.pos;
                    break;
                default:
                    context.addText(textStart, t.pos - 1);

                    if (Character.isJavaIdentifierStart(t.peek())) {
                        Tokenizer.Token ident = t.findIdent();
                        int len = ident.len;
                        Tokenizer.Token args = null;
                        if (t.isNext('(')) {
                            args = t.findMatching('(');
                            len += args.len;
                        }
                        Class<? extends  Node> orbClass = commands.get(ident.toString());
                        if (orbClass != null) {
                            try {
                                if (args != null) {
                                    context.add(orbClass.newInstance(), args.start + 1, args.len - 2);
                                } else {
                                    context.add(orbClass.newInstance(), t.pos, 0);
                                }
                                textStart = ident.start + len;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            context.add(new ExprNode(), ident.start, len);
                            textStart = ident.start + len;
                        }
                    }
                }
            default:
                break;
            }
        }
        context.addText(textStart, t.pos);
        return new Template(this, context.getRoot());
    }


    private static class Tokenizer {
        int pos = 0;
        char[] template;

        Tokenizer(char[] template) {
            this.template = template;
        }

        public boolean hasMore() {
            return pos < template.length;
        }

        public char next() {
            return template[pos++];
        }
        public char peek() {
            return template[pos];
        }

        public boolean isNext(char ch) {
            return hasMore() && peek() == ch;
        }

        public Token findIdent() {
            int start = pos;
            while (hasMore()) {
                if (Character.isJavaIdentifierPart(peek()) || peek() == '.' || peek() == '[' || peek() == ']') {
                    next();
                    continue;
                }
                break;
            }
            return new Token(start, pos - start);
        }

        public Token findMatching(char type) {
            return balancedCapture(template.length, type);
        }

        public Token balancedCapture(int end, char type) {
            int start = pos;

            int depth = 1;
            char term = type;
            switch (type) {
            case '[':
                term = ']';
                break;
            case '{':
                term = '}';
                break;
            case '(':
                term = ')';
                break;
            }

            if (type == term) {
                for (pos++; pos < end; pos++) {
                    if (template[pos] == type) {
                        return new Token(start, ++pos - start);
                    }
                }
            }
            else {
                for (pos++; pos < end; pos++) {
                    if (pos < end && template[pos] == '/') {
                        if (pos + 1 == end) {
                            return new Token(start, pos - start);
                        }
                        if (template[pos + 1] == '/') {
                            pos++;
                            while (pos < end && template[pos] != '\n') {
                                pos++;
                            }
                        }
                        else if (template[pos + 1] == '*') {
                            pos += 2;
                            SkipComment:
                                while (pos < end) {
                                    switch (template[pos]) {
                                    case '*':
                                        if (pos + 1 < end && template[pos + 1] == '/') {
                                            break SkipComment;
                                        }
                                    case '\r':
                                    case '\n':

                                        break;
                                    }
                                    start++;
                                }
                        }
                    }
                    if (pos == end) {
                        return new Token(start, pos - start);
                    }
                    if (template[pos] == '\'' || template[pos] == '"') {
                        captureStringLiteral(template[pos], end);
                    }
                    else if (template[pos] == type) {
                        depth++;
                    }
                    else if (template[pos] == term && --depth == 0) {
                        return new Token(start, ++pos - start);
                    }
                }
            }

            switch (type) {
            case '[':
                throw new CompileException("unbalanced braces [ ... ]", template, pos);
            case '{':
                throw new CompileException("unbalanced braces { ... }", template, pos);
            case '(':
                throw new CompileException("unbalanced braces ( ... )", template, pos);
            default:
                throw new CompileException("unterminated string literal", template, pos);
            }
        }

        public int captureStringLiteral(final char type, int end) {

            while (++pos < end && template[pos] != type) {
                if (template[pos] == '\\') {
                    pos++;
                }
            }

            if (pos >= end || template[pos] != type) {
                throw new CompileException("unterminated string literal", template, pos);
            }

            return pos;
        }

        class Token {
            int start;
            int len;

            public Token(int start, int len) {
                this.start = start;
                this.len = len;
            }

            public String toString() {
                return new String(template, start, len);
            }
        }
    }
}
