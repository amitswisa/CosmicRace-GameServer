package dto;

import addons.Location;

import com.google.gson.annotations.SerializedName;

public class PlayerCommand {

    public enum ePlayerAction
    {
        @SerializedName("IDLE")
        IDLE,

        @SerializedName("RUN_RIGHT")
        RUN_RIGHT,

        @SerializedName("RUN_LEFT")
        RUN_LEFT,

        @SerializedName("JUMP")
        JUMP,

        @SerializedName("DEATH")
        DEATH,

        @SerializedName("QUIT")
        QUIT,

        // TODO - ATTACK.
    }

    private String m_Username;
    private ePlayerAction m_Action;
    private Location m_Location;

    public PlayerCommand(String i_Username, ePlayerAction i_Action, Location i_Location) {
        this.m_Username = i_Username;
        this.m_Action = i_Action;
        this.m_Location = i_Location;
    }

    public String GetUsername() {
        return this.m_Username;
    }

    public ePlayerAction GetAction() {
        return this.m_Action;
    }

    public Location GetLocation() {
        return this.m_Location;
    }

    public void SetUsername(String m_Username) {
        this.m_Username = m_Username;
    }

    public void SetAction(ePlayerAction m_Action) {
        this.m_Action = m_Action;
    }

    public void SetLocation(Location m_Location) {
        this.m_Location = m_Location;
    }
}