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

import io.github.bckfnn.mvel.Template;
import io.github.bckfnn.mvel.TemplateContext;
import io.github.bckfnn.mvel.TemplateRuntime;

import java.io.IOException;

import org.mvel2.MVEL;
import org.mvel2.integration.VariableResolverFactory;

/**
 * Node for a <code>@include(..)</code> orb.
 */
public class IncludeNode extends Node {
    private Object expr;

    @Override
    public void init(TemplateContext context) {
        context.pushVariableScope();
        expr = context.compileExpression(this);
        context.popVariableScope();
    }

    @Override
    public boolean eval(TemplateRuntime runtime, Object ctx, VariableResolverFactory factory, Cback callback) {
        Object val = MVEL.executeExpression(expr, ctx, factory);

        try {
            Template template = runtime.getTemplate().getTemplateCompiler().compileResource(val.toString());
            runtime.pushExecution(getNext(), factory);
            return callback.handle(template.getRoot());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
