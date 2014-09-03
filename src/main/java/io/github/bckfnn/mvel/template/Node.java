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
package io.github.bckfnn.mvel.template;

import io.github.bckfnn.mvel.TemplateContext;
import io.github.bckfnn.mvel.TemplateRuntime;

import org.mvel2.integration.VariableResolverFactory;

/**
 * Super class of all nodes in a template.
 */
public abstract class Node {
    private int begin;
    private int len;
    private Node next;
    private char[] content;

    /**
     * Default contructor. Used by the TemplateCompiler to to create custom nodes with reflection.
     */
    public Node() {

    }

    /**
     * Initialize the node during template compilation.
     * Nodes will normally compile any orb expression and, if the node is a closing scope node 
     * (e.g. @end and @else) call the templateContext.getNestedScope().demarc() to tell the start 
     * node about this end node.
     * 
     * @param templateContext the template context.
     */
    public void init(TemplateContext templateContext) {
    }

    /**
     * Tell a node that a matching @end node was found.
     * @param templateContext the template context.
     * @param endNode the @end node.
     */
    public void demarc(TemplateContext templateContext, Node endNode) {
    }

    /**
     * @return the template content.
     */
    public char[] getContent() {
        return content;
    }

    /**
     * Set the template content.
     * @param content the content.
     */
    public void setContent(char[] content) {
        this.content = content;
    }

    /**
     * @return the start position of the orb expression.
     */
    public int getBegin() {
        return begin;
    }

    /**
     * Set the start position of the orb expression.
     * @param begin the start position.
     */
    public void setBegin(int begin) {
        this.begin = begin;
    }

    /**
     * @return the length of the orb expression.
     */
    public int getLen() {
        return len;
    }

    /**
     * Set the length of the orb expression.
     * @param len the length.
     */
    public void setLen(int len) {
        this.len = len;
    }

    /**
     * Return the next node in the node sequence.
     * The template compiler will initially assign this to the node that follow but a node's init() and demarc()
     * method may reassign the next node, so f.ex. the next node for an @if node is initially the first
     * node in the true block, after the @end node call @if.demarc(), the @is's next will point to the @end node 
     * instead.
     * @return the next node.
     */
    public Node getNext() {
        return next;
    }

    /**
     * Set the next node.
     * @param next the next node.
     */
    public void setNext(Node next) {
        this.next = next;
    }

    /**
     * Return this node by default. 
     * Use by the template compiler when assigning the next node to the previous node.
     * Is override in @end nodes to return null, so the next node in the previous node will be null.
     * @return this.
     */
    public Node getThis() {
        return this;
    }

    /**
     * Evaluate the node as runtime.
     * @param runtime the template runtime.
     * @param ctx the context object
     * @param factory the variable factory.
     * @return true if the runtime can continue evaluating the template synchronously. 
     * Return false when the runtime should yield and let some asynchronously call reschedule 
     * evaluation of the template.   
     */
    public abstract boolean eval(TemplateRuntime runtime, Object ctx, VariableResolverFactory factory);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + new String(content, begin, len) + "]";
    }
}
