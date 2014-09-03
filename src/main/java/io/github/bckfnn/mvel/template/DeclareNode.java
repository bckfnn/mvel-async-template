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

import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

/**
 * Node for a @declare(..) ... @end orb.
 * The orb expression is evaluated as the name of the declared template and the body as the content of template.
 */
public class DeclareNode extends Node {
    private Object expr;
    private Node nested;

    @Override
    public void init(TemplateContext context) {
        context.pushVariableScope();
        expr = context.compileExpression(this);
        context.pushNestedScope(this);
    }

    @Override
    public void demarc(TemplateContext context, Node endNode) {
        context.popVariableScope();
        nested = getNext();
        setNext(endNode);
        context.popNestedScope();
    }

    @Override
    public boolean eval(TemplateRuntime runtime, Object ctx, VariableResolverFactory factory) {
        Map<String, Object> locals = new HashMap<String, Object>();
        MapVariableResolverFactory localFactory = new MapVariableResolverFactory(locals, factory);

        Object val = MVEL.executeExpression(expr, ctx, localFactory);

        //System.out.println(val + " " + nested);
        runtime.addDeclared(val.toString(), nested);
        return runtime.continueWith(getNext(), factory);
    }
}
