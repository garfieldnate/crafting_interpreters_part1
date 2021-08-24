package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    // name -> is defined; when declared but not yet defined, use is not allowed: `var a = a;` etc. is not allowed
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    private enum FunctionType {
        NONE, FUNCTION, INITIALIZER, METHOD
    }

    private enum ClassType {
        NONE, CLASS
    }

    public Resolver(final Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Object visitLogicalExpr(final Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(final Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(final Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Object visitVariableExpr(final Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme()) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in its own initializer");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    /**
     * Leaf processor for this visitor; determines the scope index that the given token in the expression refers to,
     * and shares the information with the interpreter.
     *
     * @param expr
     * @param name
     */
    private void resolveLocal(final Expr expr, final Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme())) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    @Override
    public Object visitCallExpr(final Expr.Call expr) {
        resolve(expr.callee);

        for (final Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Object visitGetExpr(final Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Object visitAssignExpr(final Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Object visitLiteralExpr(final Expr.Literal expr) {
        return null;
    }

    @Override
    public Object visitBinaryExpr(final Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Object visitUnaryExpr(final Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Object visitGroupingExpr(final Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitVarStmt(final Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private void declare(final Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        final Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme())) {
            Lox.error(name, "Already a variable with this name in this scope");
        }
        scope.put(name.lexeme(), false);
    }

    private void define(final Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        scopes.peek().put(name.lexeme(), true);
    }

    @Override
    public Void visitExpressionStmt(final Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(final Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(final Stmt.Function function, final FunctionType type) {
        final FunctionType enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();

        for (final Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);

        endScope();
        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitBlockStmt(final Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(final Stmt.Class stmt) {
        final ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        beginScope();
        scopes.peek().put("this", true);

        for (final Stmt.Function method : stmt.methods) {
            final FunctionType declaration =
                    method.name.lexeme().equals("init") ? FunctionType.INITIALIZER : FunctionType.METHOD;
            resolveFunction(method, declaration);
        }
        endScope();

        currentClass = enclosingClass;
        return null;
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    void resolve(final List<Stmt> statements) {
        for (final Stmt statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(final Stmt statement) {
        statement.accept(this);
    }

    private void resolve(final Expr expression) {
        expression.accept(this);
    }

    @Override
    public Void visitIfStmt(final Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(final Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(final Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code");
        }
        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer");
            }
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(final Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }
}
