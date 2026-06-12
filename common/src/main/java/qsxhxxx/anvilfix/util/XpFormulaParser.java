package qsxhxxx.anvilfix.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全数学公式解析器
 * 仅支持严格单调的一一映射函数（一个x对应唯一y，一个y对应唯一x）
 *
 * 支持的运算符和函数：
 *   运算符：+  -  *  /  ^（幂）
 *   函数：sqrt(x)  ln(x)  log(x)  log10(x)  exp(x)
 *   常量：pi  e
 *   变量：x（代表附魔总等级）
 */
public class XpFormulaParser {

    /** 默认公式：x * 3 / 2 */
    public static final String DEFAULT_FORMULA = "x*3/2";

    private final String expression;
    private final Node ast;

    /**
     * 解析公式字符串，构建AST
     * @throws IllegalArgumentException 公式语法错误时抛出
     */
    public XpFormulaParser(String expression) {
        this.expression = expression.trim();
        Tokenizer tokenizer = new Tokenizer(this.expression);
        Parser parser = new Parser(tokenizer);
        this.ast = parser.parse();
    }

    /**
     * 计算公式的值（向下取整，保证非负）
     * @param x 附魔总等级
     * @return 经验加成值（正整数）
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

    /** 支持的函数列表（均为严格单调函数） */
    private static final Map<String, Integer> FUNCTIONS = new HashMap<>();
    static {
        FUNCTIONS.put("sqrt", 1);   // 平方根
        FUNCTIONS.put("ln", 1);     // 自然对数
        FUNCTIONS.put("log", 1);    // 以10为底的对数
        FUNCTIONS.put("log10", 1);  // 以10为底的对数（别名）
        FUNCTIONS.put("exp", 1);    // e^x
    }

    /** 常量 */
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

            // 数字（支持小数和科学计数法）
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

            // 变量 x
            if (c == 'x') {
                pos++;
                return new Token(TokenType.VARIABLE, "x", pos - 1);
            }

            // 函数名或常量名
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

            // 运算符
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

    // ==================== 语法分析（递归下降） ====================

    interface Node {
        double evaluate(double x);
    }

    static class NumberNode implements Node {
        private final double value;
        NumberNode(String val) {
            this.value = CONSTANTS.getOrDefault(val, Double.parseDouble(val));
        }
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
                case '/': return l / r;     // 除零返回 Infinity/NaN，由调用方处理
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

        /** 表达式：加法/减法 */
        Node parse() {
            Node node = parseTerm();
            while (isOp("+") || isOp("-")) {
                char op = consume().value.charAt(0);
                node = new BinaryOpNode(op, node, parseTerm());
            }
            return node;
        }

        /** 项：乘法/除法 */
        Node parseTerm() {
            Node node = parsePower();
            while (isOp("*") || isOp("/")) {
                char op = consume().value.charAt(0);
                node = new BinaryOpNode(op, node, parsePower());
            }
            return node;
        }

        /** 幂运算（右结合） */
        Node parsePower() {
            Node node = parseUnary();
            if (isOp("^")) {
                consume(); // 消耗 ^
                node = new BinaryOpNode('^', node, parsePower()); // 右结合
            }
            return node;
        }

        /** 一元负号 */
        Node parseUnary() {
            if (isOp("-")) {
                consume();
                return new UnaryMinusNode(parseUnary());
            }
            if (isOp("+")) consume(); // 一元正号直接跳过
            return parsePrimary();
        }

        /** 原子表达式：数字、变量、函数、括号 */
        Node parsePrimary() {
            Token t = peek();

            if (t.type == TokenType.NUMBER) { consume(); return new NumberNode(t.value); }
            if (t.type == TokenType.VARIABLE) { consume(); return new VariableNode(); }

            // 函数调用: func(expr)
            if (t.type == TokenType.FUNCTION) {
                String funcName = consume().value;
                expect(TokenType.LPAREN, "(");
                Node arg = parse();
                expect(TokenType.RPAREN, ")");
                return new FunctionNode(funcName, arg);
            }

            // 括号表达式
            if (t.type == TokenType.LPAREN) {
                consume();
                Node node = parse();
                expect(TokenType.RPAREN, ")");
                return node;
            }

            throw new IllegalArgumentException(
                "意外的 token: '" + t.value + "' (位置 " + t.pos + ")，期望数字、变量、函数或 '('");
        }

        private void expect(TokenType type, String desc) {
            if (peek().type != type)
                throw new IllegalArgumentException("期望 '" + desc + "'，但找到 '" + peek().value + "' (位置 " + peek().pos + ")");
            consume();
        }
    }
}
