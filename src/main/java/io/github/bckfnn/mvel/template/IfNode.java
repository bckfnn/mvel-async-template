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

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;

/**
 * Node for a <code>@if(..)</code> orb.
 */
public class IfNode extends Node {
    private Object expr;
    private Node trueBlock;
    private Node falseBlock;

    @Override
    public void init(TemplateContext context) {
        expr = context.compileExpression(this);
        context.pushNestedScope(this);
    }

    @Override
    public void demarc(TemplateContext context, Node endNode) {
        if (trueBlock == null) {
            trueBlock = getNext();
        }
        if (endNode instanceof EndNode) {
            setNext(endNode);
        } else {
            falseBlock = endNode;
        }
        context.popNestedScope();
    }

    @Override
    public boolean eval(TemplateRuntime runtime, Object ctx, VariableResolverFactory factory, Cback callback) {
        Object val = MVEL.executeExpression(expr, ctx, factory);
        runtime.pushExecution(getNext(), factory);
        return callback.handle(isTrue(val) ? trueBlock : falseBlock);
    }

    static boolean isTrue(Object val) {
        if (val == null) {
            return false;
        }
        if (val instanceof Boolean) {
            return ((Boolean) val).booleanValue();
        }
        if (val instanceof String) {
            return ((String) val).length() > 0;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        }
        return false;
    }
}
