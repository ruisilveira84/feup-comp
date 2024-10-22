package pt.up.fe.comp.initial;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.specs.util.SpecsIo;

import java.util.Collections;

public class AppTest {

    static String getResource(String filename) {
        return SpecsIo.getResource("pt/up/fe/comp/initial/apps/" + filename);
    }

    @Test
    public void testParser() {
        TestUtils.noErrors(TestUtils.parse(getResource("App1.jmm")));
    }

    @Test
    public void testFull() {
        TestUtils.noErrors(TestUtils.backend(getResource("App1.jmm"), Collections.emptyMap()));
    }

}
