package dto;

import com.google.gson.annotations.SerializedName;

public enum eMessageType {
        @SerializedName("MESSAGE")
        MESSAGE,

        @SerializedName("COMMAND")
        COMMAND,
}
