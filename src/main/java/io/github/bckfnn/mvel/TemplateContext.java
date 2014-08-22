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

import io.github.bckfnn.mvel.template.Node;
import io.github.bckfnn.mvel.template.RootNode;
import io.github.bckfnn.mvel.template.TextNode;

import java.util.Stack;

import org.mvel2.MVEL;
import org.mvel2.ParserContext;

/**
 * Context of the compilation process. The compilation context holds the globals variable use 
 * during compilation of a template string.
 */
public class TemplateContext {
    private char[] content;
    private Stack<Node> nestStack = new Stack<Node>();
    private ParserContext parserContext;

    private Node root = new RootNode();
    private Node current = root;

    /**
     * Constructor.
     * @param content the template content.
     */
    public TemplateContext(char[] content) {
        this.content = content;
        parserContext = ParserContext.create();
    }

    /**
     * @return the root node.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Add a new text node at the end of the chain.
     * @param textStart start position of the text.
     * @param end end position of the text.
     */
    void addText(int textStart, int end) {
        if (textStart < end) {
            add(new TextNode(), textStart, end - textStart);
        }
    }

    /**
     * Add a new node the chain.
     * @param node the new node.
     * @param begin start position of the ORB expression, eg @code{theOrgExpression}
     * @param len length of the orb expression.
     */
    void add(Node node, int begin, int len) {
        node.setContent(content);
        node.setBegin(begin);
        node.setLen(len);
        node.init(this);
        current.setNext(node.getThis());
        current = node;
    }

    /**
     * Compile the ORB expression from the node and the return the compiled mvel expression.
     * @param node the node whose orb expression thatc is compiled.
     * @return the compiled MVEL expression.
     */
    public Object compileExpression(Node node) {
        return MVEL.compileExpression(content, node.getBegin(), node.getLen(), parserContext);
    }

    /**
     * Push a new variable scope for the expression compiler.
     */
    public void pushVariableScope() {
        parserContext.pushVariableScope();
    }

    /**
     * Pop a variable scope.
     */
    public void popVariableScope() {
        parserContext.popVariableScope();
    }

    /**
     * Push a new nested node scope.
     * @param node the scope to remember.
     */
    public void pushNestedScope(Node node) {
        nestStack.push(node);
    }

    /**
     * Pop a nested node scope.
     */
    public void popNestedScope() {
        nestStack.pop();
    }

    /**
     * @return the last pushed node scope.
     */
    public Node getNestedScope() {
        return nestStack.peek();
    }
}
