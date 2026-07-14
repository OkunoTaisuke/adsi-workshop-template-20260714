package com.example.attendance.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkDurationTest {

    @Test
    @DisplayName("通常勤務: 09:00-18:00, 休憩60分 = 勤務480分, 残業0")
    void calculate_normalWorkDay_returnsCorrectDuration() {
        var breakRecord = BreakRecord.builder()
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(LocalTime.of(13, 0))
                .build();

        var result = WorkDuration.calculate(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                List.of(breakRecord)
        );

        assertThat(result.totalWorkMinutes()).isEqualTo(480);
        assertThat(result.totalBreakMinutes()).isEqualTo(60);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("日跨ぎ勤務: 23:00→02:00 = 勤務180分")
    void calculate_overnightShift_returnsCorrectDuration() {
        var result = WorkDuration.calculate(
                LocalTime.of(23, 0),
                LocalTime.of(2, 0),
                List.of()
        );

        assertThat(result.totalWorkMinutes()).isEqualTo(180);
        assertThat(result.totalBreakMinutes()).isEqualTo(0);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("休憩複数回の差し引き")
    void calculate_multipleBreaks_subtractsAll() {
        var break1 = BreakRecord.builder()
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(LocalTime.of(13, 0))
                .build();
        var break2 = BreakRecord.builder()
                .breakStart(LocalTime.of(15, 0))
                .breakEnd(LocalTime.of(15, 15))
                .build();

        var result = WorkDuration.calculate(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                List.of(break1, break2)
        );

        assertThat(result.totalWorkMinutes()).isEqualTo(465);
        assertThat(result.totalBreakMinutes()).isEqualTo(75);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("残業計算: 9時間勤務 = 残業60分")
    void calculate_overtime_returnsPositiveOvertime() {
        var breakRecord = BreakRecord.builder()
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(LocalTime.of(13, 0))
                .build();

        var result = WorkDuration.calculate(
                LocalTime.of(9, 0),
                LocalTime.of(19, 0),
                List.of(breakRecord)
        );

        assertThat(result.totalWorkMinutes()).isEqualTo(540);
        assertThat(result.overtimeMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("8時間以下は残業0")
    void calculate_underEightHours_noOvertime() {
        var result = WorkDuration.calculate(
                LocalTime.of(10, 0),
                LocalTime.of(17, 0),
                List.of()
        );

        assertThat(result.totalWorkMinutes()).isEqualTo(420);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("clockOutがnull → 全てnull")
    void calculate_nullClockOut_returnsNull() {
        var result = WorkDuration.calculate(
                LocalTime.of(9, 0),
                null,
                List.of()
        );

        assertThat(result.totalWorkMinutes()).isNull();
        assertThat(result.totalBreakMinutes()).isNull();
        assertThat(result.overtimeMinutes()).isNull();
    }

    @Test
    @DisplayName("未終了の休憩は計算対象外")
    void calculate_unfinishedBreak_ignored() {
        var finishedBreak = BreakRecord.builder()
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(LocalTime.of(13, 0))
                .build();
        var unfinishedBreak = BreakRecord.builder()
                .breakStart(LocalTime.of(15, 0))
                .breakEnd(null)
                .build();

        var result = WorkDuration.calculate(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                List.of(finishedBreak, unfinishedBreak)
        );

        assertThat(result.totalBreakMinutes()).isEqualTo(60);
        assertThat(result.totalWorkMinutes()).isEqualTo(480);
    }

    @Test
    @DisplayName("コアタイムチェック: 全条件OK → OK")
    void checkStatus_allGood_returnsOK() {
        var status = WorkDuration.checkStatus(
                LocalTime.of(9, 30),
                LocalTime.of(18, 30),
                480
        );

        assertThat(status).isEqualTo(AttendanceStatus.OK);
    }

    @Test
    @DisplayName("コアタイムチェック: 10:00ちょうど出勤 → OK")
    void checkStatus_exactCoreStart_returnsOK() {
        var status = WorkDuration.checkStatus(
                LocalTime.of(10, 0),
                LocalTime.of(19, 0),
                480
        );

        assertThat(status).isEqualTo(AttendanceStatus.OK);
    }

    @Test
    @DisplayName("コアタイムチェック: 遅刻 → LATE_START")
    void checkStatus_lateStart_returnsLateStart() {
        var status = WorkDuration.checkStatus(
                LocalTime.of(10, 30),
                LocalTime.of(19, 30),
                480
        );

        assertThat(status).isEqualTo(AttendanceStatus.LATE_START);
    }

    @Test
    @DisplayName("コアタイムチェック: 早退 → EARLY_LEAVE")
    void checkStatus_earlyLeave_returnsEarlyLeave() {
        var status = WorkDuration.checkStatus(
                LocalTime.of(6, 0),
                LocalTime.of(14, 30),
                480
        );

        assertThat(status).isEqualTo(AttendanceStatus.EARLY_LEAVE);
    }

    @Test
    @DisplayName("コアタイムチェック: 勤務時間不足 → SHORT_HOURS (最優先)")
    void checkStatus_shortHours_returnsShortHours() {
        var status = WorkDuration.checkStatus(
                LocalTime.of(10, 30),
                LocalTime.of(14, 0),
                200
        );

        assertThat(status).isEqualTo(AttendanceStatus.SHORT_HOURS);
    }

    @Test
    @DisplayName("コアタイムチェック: 未退勤 → null")
    void checkStatus_notClockedOut_returnsNull() {
        var status = WorkDuration.checkStatus(
                LocalTime.of(9, 0),
                null,
                null
        );

        assertThat(status).isNull();
    }
}
