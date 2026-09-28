package com.sdstiwi.gatekeeper;

public class Room {
    private String roomId;
    private String name;
    private String building;
    private String floor;
    private String deviceId;
    private int isActive;

    public Room(){}
    public Room(String roomId, String name, String building, String floor, String deviceId, int isActive) {
        this.roomId = roomId;
        this.name = name;
        this.building = building;
        this.floor = floor;
        this.deviceId = deviceId;
        this.isActive = isActive;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getName() {
        return name;
    }

    public String getBuilding() {
        return building;
    }

    public String getFloor() {
        return floor;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public int getIsActive() {
        return isActive;
    }


    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setBuilding(String building) {
        this.building = building;
    }
    public void setFloor(String floor) {
        this.floor = floor;
    }
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    public void setIsActive(int isActive) {
        this.isActive = isActive;
    }
}
