package com.chatapp.server.model;

import com.chatapp.server.enums.CallType;
import java.io.Serializable;

/**
 * Model class đại diện cho tín hiệu cuộc gọi
 */
public class CallSignal implements Serializable {
    private static final long serialVersionUID = 1L;

    private String callId;
    private Long callerId;
    private String callerUsername;
    private Long receiverId;
    private String receiverUsername;
    private CallType callType;
    private String sdpOffer;        // Session Description Protocol (đơn giản hóa)
    private String sdpAnswer;

    public CallSignal() {
    }

    public CallSignal(String callId, Long callerId, Long receiverId, CallType callType) {
        this.callId = callId;
        this.callerId = callerId;
        this.receiverId = receiverId;
        this.callType = callType;
    }

    // Getters and Setters
    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public Long getCallerId() {
        return callerId;
    }

    public void setCallerId(Long callerId) {
        this.callerId = callerId;
    }

    public String getCallerUsername() {
        return callerUsername;
    }

    public void setCallerUsername(String callerUsername) {
        this.callerUsername = callerUsername;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverUsername() {
        return receiverUsername;
    }

    public void setReceiverUsername(String receiverUsername) {
        this.receiverUsername = receiverUsername;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public String getSdpOffer() {
        return sdpOffer;
    }

    public void setSdpOffer(String sdpOffer) {
        this.sdpOffer = sdpOffer;
    }

    public String getSdpAnswer() {
        return sdpAnswer;
    }

    public void setSdpAnswer(String sdpAnswer) {
        this.sdpAnswer = sdpAnswer;
    }

    @Override
    public String toString() {
        return "CallSignal{" +
                "callId='" + callId + '\'' +
                ", callerId=" + callerId +
                ", receiverId=" + receiverId +
                ", callType=" + callType +
                '}';
    }
}
