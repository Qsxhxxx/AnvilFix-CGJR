package com.qsxhxxx.anvilxpfix.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全数学公式解析器
 * 支持运算符：+ - * / ^（幂）
 * 函数：sqrt ln log log10 exp
 * 常量：pi e
 * 变量：x（附魔总等级）
 */
public class XpFormulaParser {

    /** 默认公式：x * 3 / 2 */
    public static final String DEFAULT_FORMULA = "x*3/2";

    private final Node ast;

    public XpFormulaParser(String expression) {
        Tokenizer tokenizer = new Tokenizer(expression.trim());
        this.ast = new Parser(tokenizer).parse();
    }

    /**
     * 计算公式值（向下取整，保证非负）
     */
    public int evaluate(double x) {
        double result = ast.evaluate(x);
        return Math.max(0, (int) Math.floor(result));
    }

    // ==================== 词法分析 ====================

    enum TokenType { NUMBER, VARIABLE, FUNCTION, OPERATOR, LPAREN, RPAREN, COMMA, EOF }

    static class Token {
        final TokenType type;
        final String value;
        final int pos;

        Token(TokenType type, String value, int pos) {
            this.type = type;
            this.value = value;
            this.pos = pos;
        }
    }

    private static final Map<String, Integer> FUNCTIONS = new HashMap<>();
    static {
        FUNCTIONS.put("sqrt", 1);
        FUNCTIONS.put("ln", 1);
        FUNCTIONS.put("log", 1);
        FUNCTIONS.put("log10", 1);
        FUNCTIONS.put("exp", 1);
    }

    private static final Map<String, Double> CONSTANTS = new HashMap<>();
    static {
        CONSTANTS.put("pi", Math.PI);
        CONSTANTS.put("e", Math.E);
    }

    static class Tokenizer {
        private final String expr;
        private int pos = 0;

        Tokenizer(String expr) { this.expr = expr; }

        Token next() {
            while (pos < expr.length() && Character.isWhitespace(expr.charAt(pos))) pos++;
            if (pos >= expr.length()) return new Token(TokenType.EOF, "", pos);

            char c = expr.charAt(pos);

            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < expr.length() && (Character.isDigit(expr.charAt(pos)) || expr.charAt(pos) == '.')) pos++;
                if (pos < expr.length() && (expr.charAt(pos) == 'e' || expr.charAt(pos) == 'E')) {
                    pos++;
                    if (pos < expr.length() && (expr.charAt(pos) == '+' || expr.charAt(pos) == '-')) pos++;
                    while (pos < expr.length() && Character.isDigit(expr.charAt(pos))) pos++;
                }
                return new Token(TokenType.NUMBER, expr.substring(start, pos), start);
            }

            if (c == 'x') {
                pos++;
                return new Token(TokenType.VARIABLE, "x", pos - 1);
            }

            if (Character.isLetter(c)) {
                int start = pos;
                while (pos < expr.length() && Character.isLetterOrDigit(expr.charAt(pos))) pos++;
                String name = expr.substring(start, pos);
                if (FUNCTIONS.containsKey(name))
                    return new Token(TokenType.FUNCTION, name, start);
                if (CONSTANTS.containsKey(name))
                    return new Token(TokenType.NUMBER, name, start);
                throw new IllegalArgumentException("未知标识符: '" + name + "' (位置 " + start + ")");
            }

            pos++;
            switch (c) {
                case '+': case '-': case '*': case '/': case '^':
                    return new Token(TokenType.OPERATOR, String.valueOf(c), pos - 1);
                case '(': return new Token(TokenType.LPAREN, "(", pos - 1);
                case ')': return new Token(TokenType.RPAREN, ")", pos - 1);
                case ',': return new Token(TokenType.COMMA, ",", pos - 1);
                default:
                    throw new IllegalArgumentException("非法字符: '" + c + "' (位置 " + (pos - 1) + ")");
            }
        }
    }

    // ==================== 语法分析 ====================

    interface Node { double evaluate(double x); }

    static class NumberNode implements Node {
        private final double value;
        NumberNode(String val) { this.value = CONSTANTS.getOrDefault(val, Double.parseDouble(val)); }
        public double evaluate(double x) { return value; }
    }

    static class VariableNode implements Node {
        public double evaluate(double x) { return x; }
    }

    static class BinaryOpNode implements Node {
        private final char op;
        private final Node left, right;
        BinaryOpNode(char op, Node left, Node right) { this.op = op; this.left = left; this.right = right; }
        public double evaluate(double x) {
            double l = left.evaluate(x), r = right.evaluate(x);
            switch (op) {
                case '+': return l + r;
                case '-': return l - r;
                case '*': return l * r;
                case '/': return l / r;
                case '^': return Math.pow(l, r);
                default: throw new IllegalArgumentException("未知运算符: " + op);
            }
        }
    }

    static class UnaryMinusNode implements Node {
        private final Node child;
        UnaryMinusNode(Node child) { this.child = child; }
        public double evaluate(double x) { return -child.evaluate(x); }
    }

    static class FunctionNode implements Node {
        private final String name;
        private final Node arg;
        FunctionNode(String name, Node arg) { this.name = name; this.arg = arg; }
        public double evaluate(double x) {
            double v = arg.evaluate(x);
            switch (name) {
                case "sqrt": return Math.sqrt(v);
                case "ln":   return Math.log(v);
                case "log":
                case "log10": return Math.log10(v);
                case "exp":  return Math.exp(v);
                default: throw new IllegalArgumentException("未知函数: " + name);
            }
        }
    }

    static class Parser {
        private final Deque<Token> tokens = new ArrayDeque<>();

        Parser(Tokenizer tokenizer) {
            Token t;
            while ((t = tokenizer.next()).type != TokenType.EOF)
                tokens.addLast(t);
        }

        private Token peek() { return tokens.isEmpty() ? new Token(TokenType.EOF, "", -1) : tokens.peekFirst(); }
        private Token consume() { return tokens.isEmpty() ? new Token(TokenType.EOF, "", -1) : tokens.removeFirst(); }

        private boolean isOp(String s) {
            return peek().type == TokenType.OPERATOR && peek().value.equals(s);
        }

        Node parse() {
            Node node = parseTerm();
            while (isOp("+") || isOp("-")) {
                char op = consume().value.charAt(0);
                node = new BinaryOpNode(op, node, parseTerm());
            }
            return node;
        }

        Node parseTerm() {
            Node node = parsePower();
            while (isOp("*") || isOp("/")) {
                char op = consume().value.charAt(0);
                node = new BinaryOpNode(op, node, parsePower());
            }
            return node;
        }

        Node parsePower() {
            Node node = parseUnary();
            if (isOp("^")) {
                consume();
                node = new BinaryOpNode('^', node, parsePower());
            }
            return node;
        }

        Node parseUnary() {
            if (isOp("-")) { consume(); return new UnaryMinusNode(parseUnary()); }
            if (isOp("+")) consume();
            return parsePrimary();
        }

        Node parsePrimary() {
            Token t = peek();
            if (t.type == TokenType.NUMBER) { consume(); return new NumberNode(t.value); }
            if (t.type == TokenType.VARIABLE) { consume(); return new VariableNode(); }
            if (t.type == TokenType.FUNCTION) {
                String funcName = consume().value;
                expect(TokenType.LPAREN, "(");
                Node arg = parse();
                expect(TokenType.RPAREN, ")");
                return new FunctionNode(funcName, arg);
            }
            if (t.type == TokenType.LPAREN) {
                consume();
                Node node = parse();
                expect(TokenType.RPAREN, ")");
                return node;
            }
            throw new IllegalArgumentException("意外的 token: '" + t.value + "' (位置 " + t.pos + ")");
        }

        private void expect(TokenType type, String desc) {
            if (peek().type != type)
                throw new IllegalArgumentException("期望 '" + desc + "'，但找到 '" + peek().value + "'");
            consume();
        }
    }
}
