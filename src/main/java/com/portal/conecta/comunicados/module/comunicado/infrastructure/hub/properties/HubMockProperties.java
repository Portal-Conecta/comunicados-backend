package com.portal.conecta.comunicados.module.comunicado.infrastructure.hub.properties;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hub.mock")
public record HubMockProperties(
        List<String> classIds,
        List<String> userIds,
        List<String> roomIds,
        List<String> userRoomLinks,
        Map<String, List<MockStudent>> studentsByClass,
        Map<String, List<String>> accessibleClassIdsByUser,
        Map<String, String> userTypesById
) {

    public HubMockProperties {
        classIds = classIds == null ? List.of() : List.copyOf(classIds);
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
        roomIds = roomIds == null ? List.of() : List.copyOf(roomIds);
        userRoomLinks = userRoomLinks == null ? List.of() : List.copyOf(userRoomLinks);
        studentsByClass = studentsByClass == null ? Map.of() : Map.copyOf(studentsByClass);
        accessibleClassIdsByUser = accessibleClassIdsByUser == null ? Map.of() : Map.copyOf(accessibleClassIdsByUser);
        userTypesById = userTypesById == null ? Map.of() : Map.copyOf(userTypesById);
    }

    public record MockStudent(
            String id,
            String name
    ) {
    }
}
