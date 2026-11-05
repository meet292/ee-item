package com.grade.web;

import com.grade.model.Student;
import com.grade.service.GradeManager;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 成绩管理接口，映射到 /api/*，由 Tomcat 调用。
 * GET  /api/students?keyword=xx  查询学生
 * GET  /api/stats                班级统计
 * POST /api/save                 录入 / 修改成绩
 */
public class GradeServlet extends HttpServlet {

    private GradeManager manager;

    @Override
    public void init() {
        manager = new GradeManager();
        initSampleData();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepare(request, response);
        String path = request.getPathInfo();
        if (path == null) {
            path = "";
        }
        switch (path) {
            case "/students":
                sendStudents(request, response);
                break;
            case "/stats":
                sendStats(response);
                break;
            default:
                sendJson(response, 404, "{\"ok\":false,\"message\":\"接口不存在\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        prepare(request, response);
        String path = request.getPathInfo();
        if (path == null) {
            path = "";
        }
        if ("/save".equals(path)) {
            handleSave(request, response);
        } else {
            sendJson(response, 404, "{\"ok\":false,\"message\":\"接口不存在\"}");
        }
    }

    /** 统一设置编码与响应类型 */
    private void prepare(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
    }

    // ==================== 查询 ====================

    private void sendStudents(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String keyword = request.getParameter("keyword");
        keyword = keyword == null ? "" : keyword.trim();
        List<Student> students;
        synchronized (manager) {
            students = keyword.isEmpty()
                    ? new ArrayList<>(manager.getStudents())
                    : manager.search(keyword);
        }
        StringBuilder json = new StringBuilder("{\"ok\":true,\"students\":[");
        for (int i = 0; i < students.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(studentJson(students.get(i)));
        }
        json.append("]}");
        sendJson(response, 200, json.toString());
    }

    // ==================== 录入 / 修改 ====================

    private void handleSave(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String id = trim(request.getParameter("id"));
        if (id.isEmpty()) {
            sendJson(response, 400, "{\"ok\":false,\"message\":\"学号不能为空\"}");
            return;
        }

        String[] subjects = GradeManager.SUBJECTS;
        double[] values = new double[subjects.length];
        boolean[] provided = new boolean[subjects.length];
        for (int i = 0; i < subjects.length; i++) {
            String text = trim(request.getParameter(subjects[i]));
            if (text.isEmpty()) {
                continue;
            }
            try {
                double score = Double.parseDouble(text);
                if (score < 0 || score > 100) {
                    sendJson(response, 400,
                            "{\"ok\":false,\"message\":\"" + subjects[i] + "成绩必须在 0~100 之间\"}");
                    return;
                }
                values[i] = score;
                provided[i] = true;
            } catch (NumberFormatException e) {
                sendJson(response, 400,
                        "{\"ok\":false,\"message\":\"" + subjects[i] + "成绩请输入数字\"}");
                return;
            }
        }

        synchronized (manager) {
            Student student = manager.findById(id);
            boolean isNew = false;
            if (student == null) {
                String name = trim(request.getParameter("name"));
                if (name.isEmpty()) {
                    sendJson(response, 400, "{\"ok\":false,\"message\":\"新增学生必须填写姓名\"}");
                    return;
                }
                String className = trim(request.getParameter("className"));
                if (className.isEmpty()) {
                    className = "4A班";
                }
                student = new Student(id, name, className);
                manager.addStudent(student);
                isNew = true;
            }

            boolean changed = false;
            for (int i = 0; i < subjects.length; i++) {
                if (provided[i]) {
                    student.setScore(subjects[i], values[i]);
                    changed = true;
                }
            }
            if (!changed) {
                sendJson(response, 400, "{\"ok\":false,\"message\":\"没有填写任何成绩，未保存\"}");
                return;
            }

            String message = (isNew ? "已新增学生并保存成绩：" : "已更新成绩：")
                    + student.getName() + "（" + student.getId() + "）";
            sendJson(response, 200,
                    "{\"ok\":true,\"message\":\"" + escape(message) + "\",\"student\":"
                            + studentJson(student) + "}");
        }
    }

    // ==================== 统计 ====================

    private void sendStats(HttpServletResponse response) throws IOException {
        StringBuilder json = new StringBuilder("{\"ok\":true,\"subjects\":[");
        synchronized (manager) {
            String[] subjects = GradeManager.SUBJECTS;
            for (int i = 0; i < subjects.length; i++) {
                if (i > 0) {
                    json.append(',');
                }
                int count = manager.getSubjectScoreCount(subjects[i]);
                json.append("{\"name\":\"").append(escape(subjects[i])).append("\",")
                        .append("\"average\":").append(number(manager.getSubjectAverage(subjects[i]))).append(',')
                        .append("\"count\":").append(count).append(',')
                        .append("\"pass\":").append(manager.getSubjectPassCount(subjects[i])).append(',')
                        .append("\"passRate\":").append(number(manager.getSubjectPassRate(subjects[i]) * 100))
                        .append('}');
            }
            json.append("],\"overall\":{")
                    .append("\"average\":").append(number(manager.getOverallAverage())).append(',')
                    .append("\"passRate\":").append(number(manager.getOverallPassRate() * 100)).append(',')
                    .append("\"passCount\":").append(manager.getOverallPassCount()).append(',')
                    .append("\"totalCount\":").append(manager.getOverallScoreCount()).append(',')
                    .append("\"studentCount\":").append(manager.getStudentCount()).append(',')
                    .append("\"passLine\":").append(number(GradeManager.PASS_LINE))
                    .append("}}");
        }
        sendJson(response, 200, json.toString());
    }

    // ==================== 工具方法 ====================

    private String studentJson(Student student) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(escape(student.getId())).append("\",")
                .append("\"name\":\"").append(escape(student.getName())).append("\",")
                .append("\"className\":\"").append(escape(student.getClassName())).append("\",")
                .append("\"scores\":{");
        int index = 0;
        for (Map.Entry<String, Double> entry : student.getScores().entrySet()) {
            if (index++ > 0) {
                sb.append(',');
            }
            sb.append('"').append(escape(entry.getKey())).append("\":").append(number(entry.getValue()));
        }
        sb.append("},\"total\":").append(number(student.getTotal()))
                .append(",\"average\":").append(number(student.getAverage()))
                .append('}');
        return sb.toString();
    }

    private void sendJson(HttpServletResponse response, int status, String body) throws IOException {
        response.setStatus(status);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(body);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String number(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    // ==================== 示例数据 ====================

    private void initSampleData() {
        Student s1 = new Student("A001", "张三", "4A班");
        s1.setScore("语文", 88);
        s1.setScore("数学", 95);
        s1.setScore("英语", 79);

        Student s2 = new Student("A002", "李四", "4A班");
        s2.setScore("语文", 76);
        s2.setScore("数学", 58);
        s2.setScore("英语", 82);

        Student s3 = new Student("A003", "王五", "4A班");
        s3.setScore("语文", 91);
        s3.setScore("数学", 87);
        s3.setScore("英语", 93);

        Student s4 = new Student("A004", "赵六", "4A班");
        s4.setScore("语文", 65);
        s4.setScore("数学", 72);
        s4.setScore("英语", 55);

        Student s5 = new Student("A005", "孙七", "4A班");
        s5.setScore("语文", 59);
        s5.setScore("数学", 48);
        s5.setScore("英语", 61);

        manager.addStudent(s1);
        manager.addStudent(s2);
        manager.addStudent(s3);
        manager.addStudent(s4);
        manager.addStudent(s5);
    }
}
