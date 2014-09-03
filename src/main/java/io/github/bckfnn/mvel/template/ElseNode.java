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
 * Node for a @else orb.
 */
public class ElseNode extends Node {
    private Object expr;
    private Node trueBlock;
    private Node falseBlock;
    private Node ifNode;

    @Override
    public void init(TemplateContext context) {
        ifNode = context.getNestedScope();
        ifNode.demarc(context, this);
        context.pushNestedScope(this);;
        if (getLen() > 0) {
            expr = context.compileExpression(this);
        }
    }

    @Override
    public void demarc(TemplateContext context, Node endNode) {
        if (expr != null) {
            trueBlock = getNext();
        } else {
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
    public void setNext(Node next) {
        if (next instanceof EndNode) {
            ifNode.setNext(next);
        }
        super.setNext(next);
    }

    @Override
    public Node getThis() {
        return null;
    }

    @Override
    public boolean eval(TemplateRuntime runtime, Object ctx, VariableResolverFactory factory) {
        if (expr != null) {
            Object val = MVEL.executeExpression(expr, ctx, factory);
            runtime.pushExecution(getNext(), factory);
            return runtime.continueWith(IfNode.isTrue(val) ? trueBlock : falseBlock, factory);
        }

        runtime.pushExecution(getNext(), factory);
        return runtime.continueWith(trueBlock, factory);
    }
}
