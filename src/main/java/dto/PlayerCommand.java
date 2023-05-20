package dto;

import addons.Location;

import com.google.gson.annotations.SerializedName;

public class PlayerCommand {
    private String m_MessageType;
    private String m_Username;
    private Location m_Location;
    private String m_Action;
    public PlayerCommand(String i_MessageType, String i_Username, String i_Action, Location i_Location) {
        this.m_MessageType = i_MessageType;
        this.m_Username = i_Username;
        this.m_Action = i_Action;
        this.m_Location = i_Location;
    }

    public String GetMessageType()
    {
        return this.m_MessageType.toString();
    }

    public String GetUsername() {
        return this.m_Username;
    }

    public String GetAction() {
        return this.m_Action;
    }

    public Location GetLocation() {
        return this.m_Location;
    }

    public void SetUsername(String m_Username) {
        this.m_Username = m_Username;
    }

    public void SetAction(String m_Action) {
        this.m_Action = m_Action;
    }

    public void SetLocation(Location m_Location) {
        this.m_Location = m_Location;
    }

    public void SetMessageType(String m_MessageType) {
        this.m_MessageType = m_MessageType;
    }
}