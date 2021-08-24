package com.craftinginterpreters.tool;

import com.craftinginterpreters.lox.Lox;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateAst {
    private static final String USAGE = "Usage: generate_ast <output directory>";

    public static void main(final String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println(USAGE);
            System.exit(Lox.USAGE_ERROR_CODE);
        }
        final Path outputDir = Paths.get(args[0]);
        defineAst(outputDir, "Expr", new HashMap<>() {
            @Serial
            private static final long serialVersionUID = -994542516729871375L;

            {
                put("Assign", List.of("Token name", "Expr value"));
                put("Binary", List.of("Expr left", "Token operator", "Expr right"));
                put("Call", List.of("Expr callee", "Token paren", "List<Expr> arguments"));
                put("Get", List.of("Expr object", "Token name"));
                put("Grouping", List.of("Expr expression"));
                put("Literal", List.of("Object value"));
                put("Logical", List.of("Expr left", "Token operator", "Expr right"));
                put("Set", List.of("Expr object", "Token name", "Expr value"));
                put("This", List.of("Token keyword"));
                put("Unary", List.of("Token operator", "Expr right"));
                put("Variable", List.of("Token name"));
            }
        });
        defineAst(outputDir, "Stmt", Map.of(
                "Block", List.of("List<Stmt> statements"),
                "Class", List.of("Token name", "List<Stmt.Function> methods"),
                "Expression", List.of("Expr expression"),
                "Function", List.of("Token name", "List<Token> params", "List<Stmt> body"),
                "If", List.of("Expr condition", "Stmt thenBranch", "Stmt elseBranch"),
                "Print", List.of("Expr expression"),
                "Return", List.of("Token keyword", "Expr value"),
                "Var", List.of("Token name", "Expr initializer"),
                "While", List.of("Expr condition", "Stmt body")
        ));
    }

    private static void defineAst(final Path outputDir, final String baseName, final Map<String, List<String>> types) throws IOException {
        final Path path = outputDir.resolve(baseName + ".java");
        final PrintWriter writer = new PrintWriter(path.toFile(), StandardCharsets.UTF_8);

        writer.println("// This file is generated by GenerateAst.java. DO NOT EDIT DIRECTLY! Edit GenerateAst.java instead.");
        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");

        defineVisitor(writer, baseName, types);

        // the AST classes
        types.forEach((className, fields) -> defineType(writer, baseName, className, fields));

        // the base accept() method
        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineVisitor(final PrintWriter writer, final String baseName, final Map<String, List<String>> types) {
        writer.println("  interface Visitor<R> {");

        types.forEach((className, fields) -> {
            writer.println("    R visit" + className + baseName + "(" + className + " " + baseName.toLowerCase() + ");");
        });

        writer.println("}");
    }

    private static void defineType(final PrintWriter writer, final String baseName, final String className, final List<String> fields) {
        writer.println("\n  static class " + className + " extends " + baseName + " {");

        // constructor
        writer.println("    " + className + "(" + String.join(",", fields) + ") {");

        // store parameters in fields
        for (final String field : fields) {
            final String name = field.split(" ")[1];
            writer.println("      this." + name + " = " + name + ";");
        }
        writer.println("    }");

        // visitor patterns
        writer.println();
        writer.println("    @Override");
        writer.println("    <R> R accept(Visitor<R> visitor) {");
        writer.println("      return visitor.visit" + className + baseName + "(this);");
        writer.println("    }");

        // fields
        writer.println();
        for (final String field : fields) {
            writer.println("    final " + field + ";");
        }

        writer.println("  }");

    }
}
