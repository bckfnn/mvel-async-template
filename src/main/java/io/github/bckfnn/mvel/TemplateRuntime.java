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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.mvel2.integration.VariableResolverFactory;

/**
 * Runtime execution environment.
 */
public class TemplateRuntime {
    private Stack<Node> runtimeStack = new Stack<Node>();
    private Stack<VariableResolverFactory> namespace = new Stack<VariableResolverFactory>();
    private Map<String, Node> declared = new HashMap<String, Node>();
    private Object context;

    private Node currentNode;
    private VariableResolverFactory currentFactory;
    private Template template;

    StringBuilder output = new StringBuilder();

    /**
     * Construct a runtime environment.
     * @param template the template to run.
     * @param ctx the context object.
     * @param factory a variable factory.
     */
    public TemplateRuntime(Template template, Object ctx, VariableResolverFactory factory) {
        this.template = template;

        context = ctx;
        currentNode = template.getRoot();
        currentFactory = factory;
    }

    /**
     * Execute the template.
     */
    public void exec() {
        while (true) {
            //System.out.println("handle:" + currentNode);
            if (!currentNode.eval(this, context, currentFactory)) {
                break;
            }
            while (currentNode == null && runtimeStack.size() > 0) {
                currentNode = runtimeStack.pop();
                currentFactory = namespace.pop();
            }
            if (currentNode == null) {
                end();
                break;
            }
        }
    }

    public boolean continueWith(Node node, VariableResolverFactory factory) {
        currentNode = node;
        currentFactory = factory;
        return true;
    }

    /**
     * Save an old execution level on the stack. Execution will return to the old level when some future 
     * node does <code>runtime.continueWith(null, ?)</code>
     * @param node the node where execution will continue eventually.
     * @param vrf the current variable factory.
     */
    public void pushExecution(Node node, VariableResolverFactory vrf) {
        runtimeStack.push(node);
        namespace.push(vrf);
    }

    /**
     * Generate template output. 
     * @param str the string output.
     * @param next the node where execution must continue when output have been written.
     * @param factory the variable factory.
     * @return true if execution can continue synchronously.
     */
    public boolean append(String str, Node next, VariableResolverFactory factory) {
        output.append(str);
        return continueWith(next, factory);
    }

    /**
     * @param content the string output.
     * @param start the start position of the output characters.
     * @param len the length of output.
     * @param next the node where execution must continue when output have been written.
     * @param factory the variable factory.
     * @return true if execution can continue synchronously.
     */
    public boolean append(char[] content, int start, int len, Node next, VariableResolverFactory factory) {
        output.append(content, start, len);
        return continueWith(next, factory);
    }

    public void end() {
        
    }
    
    /**
     * @return the generated output.
     */
    public String getOutput() {
        return output.toString();
    }

    /**
     * Return the template being executed.
     * @return the template.
     */
    public Template getTemplate() {
        return template;
    }
    
    /**
     * Register a sub template with a name. 
     * @param name the name of the sub template. After being declared the sub template can be used with the @includeNames(..) orb.
     * @param subtemplate the initial node of the sub template.
     */
    public void addDeclared(String name, Node subtemplate) {
        declared.put(name, subtemplate);
    }
    
    /**
     * Return the initial node of a previously declared sub template.
     * @param name the name of the sub template.
     * @return the sub template's start node.
     */
    public Node getDeclared(String name) {
        return declared.get(name);
    }
}
