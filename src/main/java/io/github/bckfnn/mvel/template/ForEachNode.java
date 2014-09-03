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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

/**
 * Node for a <code>@foreach(..)</code> orb.
 */
public class ForEachNode extends Node {
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
        String key = "$";

        if (locals.size() == 1) {
            key = locals.keySet().iterator().next();
            val = locals.get(key);
        }

        if (val instanceof Collection) {
            iterate(runtime, key, ((Collection<?>) val).iterator(), factory);
            return runtime.continueWith(null, factory);
        }
        if (val.getClass().isArray()) {
            iterate(runtime, key, Arrays.asList((Object[]) val).iterator(), factory);
            return runtime.continueWith(null, factory);
        }
        return runtime.continueWith(getNext(), factory);
    }

    private void iterate(TemplateRuntime runtime, String key, Iterator<?> iterator, VariableResolverFactory factory) {
        Map<String, Object> locals = new HashMap<String, Object>();
        MapVariableResolverFactory localFactory = new MapVariableResolverFactory(locals, factory);
        runtime.pushExecution(new EachIteratorNode(key, iterator,  locals), localFactory);
    }

    class EachIteratorNode extends Node {
        String key;
        Iterator<?> iterator;
        Map<String, Object> locals;

        public EachIteratorNode(String key, Iterator<?> iterator, Map<String, Object> locals) {
            this.key = key;
            this.iterator = iterator;
            this.locals = locals;
        }

        public boolean eval(TemplateRuntime runtime, Object ctx, VariableResolverFactory factory) {
            if (iterator.hasNext()) {
                Object val = iterator.next();
                locals.put(key, val);

                runtime.pushExecution(this,  factory);
                return runtime.continueWith(nested, factory);
            } else {
                return runtime.continueWith(ForEachNode.this.getNext(), factory);
            }
        }

        public String toString() {
            return "ForeachIterable";
        }
    }
}
