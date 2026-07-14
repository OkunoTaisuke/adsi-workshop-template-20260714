package com.example.attendance.infrastructure.exception;

import org.springframework.http.HttpStatus;

public class AttendanceException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AttendanceException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static AttendanceException alreadyClockedIn() {
        return new AttendanceException(HttpStatus.CONFLICT, "ALREADY_CLOCKED_IN", "本日は既に出勤打刻済みです");
    }

    public static AttendanceException notClockedIn() {
        return new AttendanceException(HttpStatus.BAD_REQUEST, "NOT_CLOCKED_IN", "出勤打刻がありません");
    }

    public static AttendanceException breakNotEnded() {
        return new AttendanceException(HttpStatus.BAD_REQUEST, "BREAK_NOT_ENDED", "未終了の休憩があります");
    }

    public static AttendanceException noActiveBreak() {
        return new AttendanceException(HttpStatus.BAD_REQUEST, "NO_ACTIVE_BREAK", "終了可能な休憩がありません");
    }

    public static AttendanceException notCurrentMonth() {
        return new AttendanceException(HttpStatus.BAD_REQUEST, "NOT_CURRENT_MONTH", "当月分のみ修正可能です");
    }

    public static AttendanceException forbidden() {
        return new AttendanceException(HttpStatus.FORBIDDEN, "FORBIDDEN", "この操作は許可されていません");
    }

    public static AttendanceException notFound() {
        return new AttendanceException(HttpStatus.NOT_FOUND, "NOT_FOUND", "記録が見つかりません");
    }
}
