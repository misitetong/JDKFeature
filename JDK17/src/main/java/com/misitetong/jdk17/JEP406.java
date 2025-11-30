package com.misitetong.jdk17;

/**
 * @author zyy
 * @Date 2025/11/30 16:22
 * @Description <a href="https://openjdk.org/jeps/406">com.misitetong.jdk17.JEP406</a>
 */
public class JEP406 {

    public static void main(String[] args) {
        System.out.println(formatterPatternSwitch("1"));
        System.out.println(formatterPatternSwitch(true));
        System.out.println(eval(new Const(1)));
    }

    public static String formatterPatternSwitch(Object o) {
        return switch (o) {
            // 1. 模式用作case标签
            case Integer i -> String.format("int %d", i);
            case Long l -> String.format("long %d", l);
            case Double d -> String.format("double %f", d);
            // 2. 支持 null 作为 case 标签
            case null, String s  -> "String s or null:" + s;
            // 3 && 4. 支持守卫模式和括号
            case (Boolean b && b.booleanValue() == true)  -> String.format("boolean %s", b);
            default -> o.toString();
        };
    }

    sealed interface Expr permits Const, Add {}
    record Const(int val) implements Expr {}
    record Add(Expr left, Expr right) implements Expr {}

    static int eval(Expr e) {
        // 5. 增强的完整性检查
        return switch (e) {
            case Const c -> c.val();
            case Add a   -> eval(a.left()) + eval(a.right());
        };
    }
}
