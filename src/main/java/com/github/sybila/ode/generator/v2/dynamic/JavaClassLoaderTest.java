package com.github.sybila.ode.generator.v2.dynamic;

import com.github.sybila.ode.generator.rect.Rectangle;
import com.github.sybila.ode.generator.v2.dynamic.JavaClassLoader;
import com.github.sybila.ode.model.Evaluable;
import com.github.sybila.ode.model.OdeModel;
import com.github.sybila.ode.model.Parser;
import com.github.sybila.ode.model.Summand;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class JavaClassLoaderTest {

    private static final String FULL_CLASS_PATH = "C:\\Users\\Jakub\\Desktop\\ode-generator\\build\\libs\\ode-generator-1.3.3-2-all.jar";
    private static final String CLASS_CODE = "import com.github.sybila.checker.Solver;\n" +
            "import com.github.sybila.ode.generator.rect.Rectangle;\n" +
            "import com.github.sybila.ode.model.OdeModel;\n" +
            "import com.github.sybila.ode.generator.v2.dynamic.OnTheFlyColorComputer;\n" +
            "\n" +
            "import java.util.Set;\n" +
            "\n" +
            "public class TestClass implements OnTheFlyColorComputer<Set<Rectangle>> {\n" +
            "\n" +
            "    @Override\n" +
            "    public void initialize(OdeModel model, Solver<Set<Rectangle>> solver) {\n" +
            "        System.out.println(\"Initializing...\");\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public Set<Rectangle> getVertexColor(int vertex, int dimension, boolean positive) {\n" +
            "        System.out.println(\"Compute vertex color!\");\n" +
            "        return null;\n" +
            "    }\n" +
            "    \n" +
            "}";

    public static void main(String[] args) {
        try {
            Path project = Files.createTempDirectory("on-the-fly");

            Path sourceCodePath = project.resolve("TestClass.java");
            BufferedWriter writer = Files.newBufferedWriter(sourceCodePath);
            writer.write(CLASS_CODE);
            writer.close();

            System.out.println("Temp file created: "+sourceCodePath);
            Process compiler = Runtime.getRuntime().exec(new String[]{ "javac", "-cp", FULL_CLASS_PATH, sourceCodePath.toAbsolutePath().toString() });
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(compiler.getErrorStream()));
            errorReader.lines().forEach(s -> System.err.println("CP: "+s));
            int resultCode = compiler.waitFor();
            System.out.println("Temp file compiled: "+resultCode);
            //}

            URL classUrl = project.toUri().toURL();
            System.out.println("Load dynamic from: "+classUrl);
            ClassLoader loader = new URLClassLoader(new URL[]{ classUrl });

            Class<?> dynamicClass = loader.loadClass("TestClass");

            OnTheFlyColorComputer<Set<Rectangle>> computer = (OnTheFlyColorComputer<Set<Rectangle>>) dynamicClass.newInstance();

            computer.initialize(null, null);
            computer.getVertexColor(0 ,0, true);

            OdeModel model = new Parser().parse(new File("models/model_31_reduced.bio"));

            List<Summand> equation = model.getVariables().get(0).getEquation();
            System.out.println(prepareSummands(equation));
            System.out.println(compileDerivationValueAndDenominator(equation));


            /*
            System.out.println(prepareSummands(equation));
            for (int i=0; i < equation.size(); i++) {
                System.out.println("Summand "+i+": "+compileSummand(equation.get(i), i));
            }*/

        } catch (IOException | IllegalAccessException | InstantiationException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            // delete .class file afterwards!
        }

        /*DynamicParamsOdeTransitionSystem transitionSystem = new DynamicParamsOdeTransitionSystem(new Parser()
                .parse(new File("models/tcbb.bio")), FULL_CLASS_PATH);
        */
    }

    private static String prepareSummands(List<Summand> equation) {
        StringBuilder result = new StringBuilder();
        for (int i=0; i<equation.size(); i++) {
            result.append("Summand summand")
            .append(i)
            .append(" = equation.get(")
            .append(i)
            .append(");\n");
        }
        return result.toString();
    }

    private static String compileSummand(Summand summand, int summandIndex) {
        StringBuilder result = new StringBuilder();
        for (int v : summand.getVariableIndices()) {
            result.append("varValue(vertex, ")
            .append(v)
            .append(") * ");
        }

        List<Evaluable> evaluable = summand.getEvaluable();
        for (int i = 0; i < evaluable.size(); i++) {
            Evaluable eval = evaluable.get(i);
            result.append("summand")
            .append(summandIndex)
            .append(".getEvaluable(")
            .append(i)
            .append(").invoke(varValue(vertex, ")
            .append(eval.getVarIndex())
            .append(")) * ");
        }

        result.append(summand.getConstant());
        return result.toString();
    }

    private static String compileDerivationValueAndDenominator(List<Summand> equation) {
        StringBuilder result = new StringBuilder();
        StringBuilder derivationValue = new StringBuilder();
        StringBuilder denominator = new StringBuilder();

        int paramIndex = -1;

        for (int i = 0; i < equation.size(); i++) {
            if (equation.get(i).hasParam()) {
                paramIndex = equation.get(i).getParamIndex();
                denominator.append(compileSummand(equation.get(i), i))
                        .append(" + ");
            } else {
                derivationValue.append(compileSummand(equation.get(i), i))
                        .append(" + ");
            }
        }

        if (derivationValue.length() > 0) {
            // getting rid of " + " at the end
            derivationValue.setLength(derivationValue.length() - 3);
            result.append("derivationValue = ")
                    .append(derivationValue)
                    .append(";\n");
        }

        if (denominator.length() > 0) {
            // getting rid of " + " at the end
            denominator.setLength(denominator.length() - 3);
            result.append("denominator = ")
                    .append(denominator)
                    .append(";\n")
                    .append("paramIndex = ")
                    .append(paramIndex)
                    .append(";\n");
        }

        return result.toString();
    }
}