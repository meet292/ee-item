package com.grade.service;

import com.grade.model.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * 成绩管理业务类：负责学生的增删查、成绩录入以及平均分、及格率统计。
 */
public class GradeManager {

    /** 及格线 */
    public static final double PASS_LINE = 60.0;

    /** 本系统开设的科目 */
    public static final String[] SUBJECTS = {"语文", "数学", "英语"};

    private final List<Student> students = new ArrayList<>();

    /** 新增学生，学号重复时返回 false */
    public boolean addStudent(Student student) {
        if (findById(student.getId()) != null) {
            return false;
        }
        students.add(student);
        return true;
    }

    public Student findById(String id) {
        for (Student student : students) {
            if (student.getId().equals(id)) {
                return student;
            }
        }
        return null;
    }

    /** 按学号或姓名模糊查询，返回所有匹配的学生 */
    public List<Student> search(String keyword) {
        List<Student> result = new ArrayList<>();
        for (Student student : students) {
            if (student.getId().contains(keyword) || student.getName().contains(keyword)) {
                result.add(student);
            }
        }
        return result;
    }

    public List<Student> getStudents() {
        return students;
    }

    public int getStudentCount() {
        return students.size();
    }

    /** 某科目的平均分（只统计已录入该科成绩的学生），无数据返回 0 */
    public double getSubjectAverage(String subject) {
        double total = 0;
        int count = 0;
        for (Student student : students) {
            if (student.hasScore(subject)) {
                total += student.getScore(subject);
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    /** 某科目已录入成绩的人数 */
    public int getSubjectScoreCount(String subject) {
        int count = 0;
        for (Student student : students) {
            if (student.hasScore(subject)) {
                count++;
            }
        }
        return count;
    }

    /** 某科目及格人数 */
    public int getSubjectPassCount(String subject) {
        int count = 0;
        for (Student student : students) {
            if (student.hasScore(subject) && student.getScore(subject) >= PASS_LINE) {
                count++;
            }
        }
        return count;
    }

    /** 某科目及格率，无数据返回 0 */
    public double getSubjectPassRate(String subject) {
        int count = getSubjectScoreCount(subject);
        return count == 0 ? 0 : (double) getSubjectPassCount(subject) / count;
    }

    /** 班级所有已录入成绩的平均分 */
    public double getOverallAverage() {
        double total = 0;
        int count = 0;
        for (Student student : students) {
            for (double score : student.getScores().values()) {
                total += score;
                count++;
            }
        }
        return count == 0 ? 0 : total / count;
    }

    /** 班级所有已录入成绩的及格率 */
    public double getOverallPassRate() {
        int total = 0;
        int pass = 0;
        for (Student student : students) {
            for (double score : student.getScores().values()) {
                total++;
                if (score >= PASS_LINE) {
                    pass++;
                }
            }
        }
        return total == 0 ? 0 : (double) pass / total;
    }

    /** 班级所有已录入成绩的及格人数（按人次统计） */
    public int getOverallScoreCount() {
        int total = 0;
        for (Student student : students) {
            total += student.getScores().size();
        }
        return total;
    }

    /** 班级所有已录入成绩的及格人次 */
    public int getOverallPassCount() {
        int pass = 0;
        for (Student student : students) {
            for (double score : student.getScores().values()) {
                if (score >= PASS_LINE) {
                    pass++;
                }
            }
        }
        return pass;
    }
}
