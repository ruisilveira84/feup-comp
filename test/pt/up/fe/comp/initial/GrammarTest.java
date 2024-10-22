/**
 * Copyright 2022 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.comp.initial;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;

public class GrammarTest {


    private static final String INSTANCE_METHOD = "methodDecl";
    private static final String STATEMENT = "stmt";
    private static final String EXPRESSION = "expr";


    @Test
    public void testClass() {
        TestUtils.parseVerbose("class Foo {}");
    }


    @Test
    public void testInstanceMethodEmpty() {
        TestUtils.parseVerbose("int foo(int anInt) {return anInt;}",
                INSTANCE_METHOD);
    }

    @Test
    public void testStmtAssign() {
        TestUtils.parseVerbose("a=b;", STATEMENT);
    }

    @Test
    public void testExprId() {
        TestUtils.parseVerbose("a", EXPRESSION);
    }

    @Test
    public void testExprIntLiteral() {
        TestUtils.parseVerbose("9", EXPRESSION);
    }

    @Test
    public void testExprMult() {
        TestUtils.parseVerbose("2 * 3", EXPRESSION);
    }


    @Test
    public void testExprMultAddChain() {
        TestUtils.parseVerbose("1 * 2 + 3 * 4", EXPRESSION);
    }

    @Test
    public void testExprAdd() {
        TestUtils.parseVerbose("2 + 3", EXPRESSION);
    }


}
