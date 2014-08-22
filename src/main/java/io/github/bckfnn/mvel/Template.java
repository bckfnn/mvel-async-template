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

/**
 * A compiled mvel-async template, ready for execution.
 */
public class Template {
    private Node root;
    private TemplateCompiler compiler;

    /**
     * Constructor.
     * @param compiler the template compiler. Used when included templates needs to be compiled.
     * @param root the root node.
     */
    public Template(TemplateCompiler compiler, Node root) {
        this.compiler = compiler;
        this.root = root;
    }

    /**
     * @return the root node.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * @return the template compiler.
     */
    public TemplateCompiler getTemplateCompiler() {
        return compiler;
    }
}
