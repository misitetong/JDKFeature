package com.misitetong.jdk17;

/**
 * @author zyy
 * @Date 2025/11/30 18:30
 * @Description <a href="https://openjdk.org/jeps/409">com.misitetong.jdk17.JEP409</a>
 */
public class JEP409 {

    public static void main(String[] args) {
        Animal animal = new Animal();
        System.out.println(animal.call());
        Animal dog = new Dog();
        System.out.println(dog.call());
        Animal cat = new Cat();
        System.out.println(cat.call());
        Bird bird = new Bird();
        System.out.println(bird.call());
        Animal parrot =  new Parrot();
        System.out.println(parrot.call());

        System.out.println(eval(parrot));
        System.out.println(eval(null));
    }

    static String eval(Animal animal) {
        return switch (animal) {
            case Dog dog -> dog.call();
            case Cat cat -> cat.call();
            case Bird bird -> bird.call();
            case null, default -> "null call";
        };
    }

    sealed static class Animal permits Dog, Cat, Bird {
        public String call() {
            return "Animal call";
        }
    }
    final static class Dog extends Animal {
        @Override
        public String call() {
            return "Dog call";
        }
    }
    non-sealed static class Cat extends Animal {
        @Override
        public String call() {
            return "Cat call";
        }
    }
    sealed static class Bird extends Animal {
        @Override
        public String call() {
            return "Bird call";
        }
    }
    non-sealed static class Parrot extends Bird {
        @Override
        public String call() {
            return "Parrot call";
        }
    }
}
