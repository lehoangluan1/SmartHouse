package com.java.controller.dto;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlCommandResponse {
    private Long id;
    private Long deviceId;
    private String target;
    private String value;
    private Long actorId;
    private String actorName;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime sentAt;
    private OffsetDateTime ackAt;
}