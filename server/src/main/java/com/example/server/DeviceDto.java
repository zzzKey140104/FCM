package com.example.server;

public class DeviceDto {
    public final int id;
    public final String token;
    public final String label;

    public DeviceDto(int id, String token, String label) {
        this.id = id;
        this.token = token;
        this.label = label;
    }
}


