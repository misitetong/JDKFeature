package com.misitetong.jdk17;

import jdk.incubator.foreign.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_POINTER;

/**
 * @author zyy
 * @Date 2025/12/10 22:43
 * @Description <a href="https://openjdk.org/jeps/412">com.misitetong.jdk17.JEP412</a>
 */
public class JEP412 {

    public static void main(String[] args) throws Throwable {
        // 获取链接库中的strlen函数
        MemoryAddress strlen = CLinker.systemLookup().lookup("strlen").get();
        // 获取方法的handle
        MethodHandle strlenHandle = CLinker.getInstance().downcallHandle(
                strlen,
                MethodType.methodType(int.class, MemoryAddress.class),
                FunctionDescriptor.of(C_INT, C_POINTER)
        );
        // 获取字符串
        var string = CLinker.toCString("Hello World!!", ResourceScope.newImplicitScope());
        // 调用C中的strlen函数
        System.out.println(strlenHandle.invoke(string.address()));
    }
}
