package com.stayvida.backend.dto;

public class RoomBasicDTO {
    private String roomId;
    private Integer room_NO;

    public RoomBasicDTO(String roomId, Integer room_NO) {
        this.roomId = roomId;
        this.room_NO = room_NO;
    }

    // getters & setters
    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public Integer getRoom_NO() {
        return room_NO;
    }

    public void setRoom_NO(Integer room_NO) {
        this.room_NO = room_NO;
    }
}
