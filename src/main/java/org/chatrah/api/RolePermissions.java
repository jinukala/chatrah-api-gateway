package org.chatrah.api;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class RolePermissions {

    private static final Map<String, List<String>> MAP = Map.of(

            "SYS_ADMIN", List.of("*"),

            "PRINCIPAL", List.of(
                    "/api/students", "/api/teachers", "/api/classes",
                    "/api/attendance", "/api/exams", "/api/fees",
                    "/api/salary", "/api/analytics", "/api/events",
                    "/api/blogs", "/api/notifications", "/api/access-requests",
                    "/api/school", "/api/leaves", "/api/payments",
                    "/api/teacher-leaves"
            ),

            "CLERK", List.of(
                    "/api/students", "/api/classes", "/api/fees",
                    "/api/attendance", "/api/access-requests",
                    "/api/leaves", "/api/payments"
            ),

            "TEACHER", List.of(
                    "/api/attendance", "/api/exams", "/api/classes",
                    "/api/blogs", "/api/leaves", "/api/students",
                    "/api/fees", "/api/events", "/api/teachers",
                    "/api/class-materials", "/api/quizzes",
                    "/api/teacher-leaves"
            ),

            "STUDENT", List.of(
                    "/api/fees", "/api/exams", "/api/attendance",
                    "/api/blogs", "/api/leaves", "/api/events",
                    "/api/students/birthdays", "/api/students/",
                    "/api/teachers", "/api/classes",
                    "/api/class-materials", "/api/quizzes"
            )
    );

    public boolean allowed(String role, String path) {
        if ("SYS_ADMIN".equals(role)) return true;
        if (path.startsWith("/api/auth/me")) return true;
        if (path.startsWith("/api/auth/password/change")) return true;

        return MAP.getOrDefault(role, List.of())
                .stream()
                .anyMatch(path::startsWith);
    }
}
