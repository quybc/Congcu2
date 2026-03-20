package com.example.demo_3001.model;

import java.util.Arrays;

public enum RoleCode {
    ADMIN(1L, "ADMIN"),
    MANAGER(2L, "MANAGER"),
    USER(3L, "USER");

    private final Long id;
    private final String name;

    RoleCode(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static RoleCode fromId(Long id) {
        return Arrays.stream(values())
                .filter(roleCode -> roleCode.id.equals(id))
                .findFirst()
                .orElse(USER);
    }
}
