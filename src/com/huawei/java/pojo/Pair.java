package com.huawei.java.pojo;

public class Pair<E,T> {
    private E e1;
    private T e2;

    public Pair(E e1, T e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    public E getE1() {
        return e1;
    }

    public void setE1(E e1) {
        this.e1 = e1;
    }

    public T getE2() {
        return e2;
    }

    public void setE2(T e2) {
        this.e2 = e2;
    }
}
