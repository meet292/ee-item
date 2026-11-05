package com.grade.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学生实体类，保存基本信息与各科成绩。
 */
public class Student {

    private final String id;
    private String name;
    private final String className;

    /** 科目 -> 分数，按录入顺序保存 */
    private final Map<String, Double> scores = new LinkedHashMap<>();

    public Student(String id, String name, String className) {
        this.id = id;
        this.name = name;
        this.className = className;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, Double> getScores() {
        return scores;
    }

    /** 设置某一科的分数 */
    public void setScore(String subject, double score) {
        scores.put(subject, score);
    }

    public Double getScore(String subject) {
        return scores.get(subject);
    }

    public boolean hasScore(String subject) {
        return scores.containsKey(subject);
    }

    /** 已录入科目的总分 */
    public double getTotal() {
        double total = 0;
        for (double score : scores.values()) {
            total += score;
        }
        return total;
    }

    /** 已录入科目的平均分，无成绩时返回 0 */
    public double getAverage() {
        return scores.isEmpty() ? 0 : getTotal() / scores.size();
    }

    @Override
    public String toString() {
        return id + "\t" + name + "\t" + className;
    }
}
