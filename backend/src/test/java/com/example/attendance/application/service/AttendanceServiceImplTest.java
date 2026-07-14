package com.example.attendance.application.service;

import com.example.attendance.application.dto.AttendanceUpdateRequest;
import com.example.attendance.application.dto.BreakEndRequest;
import com.example.attendance.application.dto.BreakStartRequest;
import com.example.attendance.application.dto.BreakUpdateRequest;
import com.example.attendance.application.dto.ClockInRequest;
import com.example.attendance.application.dto.ClockOutRequest;
import com.example.attendance.domain.model.AttendanceRecord;
import com.example.attendance.domain.model.AttendanceStatus;
import com.example.attendance.domain.model.BreakRecord;
import com.example.attendance.domain.model.Department;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.Role;
import com.example.attendance.domain.repository.AttendanceRecordRepository;
import com.example.attendance.domain.repository.AttendanceRevisionRepository;
import com.example.attendance.domain.repository.BreakRecordRepository;
import com.example.attendance.infrastructure.exception.AttendanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private BreakRecordRepository breakRecordRepository;
    @Mock
    private AttendanceRevisionRepository revisionRepository;

    private AttendanceServiceImpl service;
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = new AttendanceServiceImpl(attendanceRecordRepository, breakRecordRepository, revisionRepository);
        employee = Employee.builder()
                .id(1L)
                .email("tanaka@example.com")
                .name("田中太郎")
                .role(Role.EMPLOYEE)
                .department(Department.builder().id(1L).name("開発部").build())
                .build();
    }

    @Test
    @DisplayName("clockIn: 正常に出勤記録が作成される")
    void clockIn_normal_createsRecord() {
        var request = new ClockInRequest(LocalTime.of(9, 0));
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> {
                    var record = invocation.getArgument(0, AttendanceRecord.class);
                    record.setId(1L);
                    record.setBreakRecords(new ArrayList<>());
                    return record;
                });

        var result = service.clockIn(employee, request);

        assertThat(result.clockIn()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.date()).isEqualTo(LocalDate.now());
        verify(attendanceRecordRepository).save(any(AttendanceRecord.class));
    }

    @Test
    @DisplayName("clockIn: 同日二重出勤で例外")
    void clockIn_alreadyClockedIn_throwsConflict() {
        var request = new ClockInRequest(LocalTime.of(9, 0));
        var existing = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(8, 0))
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.clockIn(employee, request))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("既に出勤打刻済み");
    }

    @Test
    @DisplayName("clockOut: 正常に退勤記録 + 勤務時間計算")
    void clockOut_normal_recordsAndCalculates() {
        var request = new ClockOutRequest(LocalTime.of(18, 0));
        var breakRecord = BreakRecord.builder()
                .id(1L)
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(LocalTime.of(13, 0))
                .build();
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .breakRecords(new ArrayList<>(List.of(breakRecord)))
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.clockOut(employee, request);

        assertThat(result.clockOut()).isEqualTo(LocalTime.of(18, 0));
        assertThat(result.totalWorkMinutes()).isEqualTo(480);
        assertThat(result.totalBreakMinutes()).isEqualTo(60);
        assertThat(result.overtimeMinutes()).isEqualTo(0);
    }

    @Test
    @DisplayName("clockOut: 出勤前に退勤で例外")
    void clockOut_notClockedIn_throwsBadRequest() {
        var request = new ClockOutRequest(LocalTime.of(18, 0));
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clockOut(employee, request))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("出勤打刻がありません");
    }

    @Test
    @DisplayName("clockOut: 未終了休憩ありで例外")
    void clockOut_breakNotEnded_throwsBadRequest() {
        var request = new ClockOutRequest(LocalTime.of(18, 0));
        var unfinishedBreak = BreakRecord.builder()
                .id(1L)
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(null)
                .build();
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .breakRecords(new ArrayList<>(List.of(unfinishedBreak)))
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.clockOut(employee, request))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("未終了の休憩");
    }

    @Test
    @DisplayName("breakStart: 正常に休憩記録")
    void breakStart_normal_createsBreakRecord() {
        var request = new BreakStartRequest(LocalTime.of(12, 0));
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .breakRecords(new ArrayList<>())
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));
        when(breakRecordRepository.save(any(BreakRecord.class)))
                .thenAnswer(invocation -> {
                    var br = invocation.getArgument(0, BreakRecord.class);
                    br.setId(1L);
                    return br;
                });
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.breakStart(employee, request);

        assertThat(result.breaks()).hasSize(1);
        assertThat(result.breaks().getFirst().breakStart()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("breakStart: 未出勤で例外")
    void breakStart_notClockedIn_throwsBadRequest() {
        var request = new BreakStartRequest(LocalTime.of(12, 0));
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.breakStart(employee, request))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("出勤打刻がありません");
    }

    @Test
    @DisplayName("breakStart: 未終了休憩ありで例外")
    void breakStart_breakNotEnded_throwsBadRequest() {
        var request = new BreakStartRequest(LocalTime.of(15, 0));
        var unfinishedBreak = BreakRecord.builder()
                .id(1L)
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(null)
                .build();
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .breakRecords(new ArrayList<>(List.of(unfinishedBreak)))
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.breakStart(employee, request))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("未終了の休憩");
    }

    @Test
    @DisplayName("breakEnd: 正常に休憩終了")
    void breakEnd_normal_endsBreak() {
        var request = new BreakEndRequest(LocalTime.of(13, 0));
        var activeBreak = BreakRecord.builder()
                .id(1L)
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(null)
                .build();
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .breakRecords(new ArrayList<>(List.of(activeBreak)))
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));
        when(breakRecordRepository.save(any(BreakRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.breakEnd(employee, request);

        assertThat(result.breaks().getFirst().breakEnd()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    @DisplayName("update: 打刻修正 + Revision作成")
    void update_normal_updatesAndCreatesRevision() {
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .clockOut(LocalTime.of(18, 0))
                .totalWorkMinutes(480)
                .totalBreakMinutes(60)
                .overtimeMinutes(0)
                .breakRecords(new ArrayList<>())
                .build();
        var request = new AttendanceUpdateRequest(
                LocalTime.of(9, 15),
                LocalTime.of(18, 0),
                List.of()
        );

        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateAttendance(employee, 1L, request);

        assertThat(result.clockIn()).isEqualTo(LocalTime.of(9, 15));
        verify(revisionRepository).save(any());
    }

    @Test
    @DisplayName("update: clockInをnullにして無効化")
    void update_nullClockIn_invalidates() {
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .clockOut(LocalTime.of(18, 0))
                .totalWorkMinutes(480)
                .totalBreakMinutes(60)
                .overtimeMinutes(0)
                .breakRecords(new ArrayList<>())
                .build();
        var request = new AttendanceUpdateRequest(null, null, List.of());

        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateAttendance(employee, 1L, request);

        assertThat(result.clockIn()).isNull();
        assertThat(result.totalWorkMinutes()).isNull();
    }

    @Test
    @DisplayName("update: 当月以外の修正で例外")
    void update_notCurrentMonth_throwsBadRequest() {
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now().minusMonths(1))
                .clockIn(LocalTime.of(9, 0))
                .breakRecords(new ArrayList<>())
                .build();
        var request = new AttendanceUpdateRequest(LocalTime.of(9, 15), null, List.of());

        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.updateAttendance(employee, 1L, request))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("当月分のみ");
    }

    @Test
    @DisplayName("update: 他人の記録修正で例外")
    void update_otherEmployee_throwsForbidden() {
        var otherEmployee = Employee.builder().id(2L).name("佐藤花子").build();
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(otherEmployee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .breakRecords(new ArrayList<>())
                .build();
        var request = new AttendanceUpdateRequest(LocalTime.of(9, 15), null, List.of());

        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.updateAttendance(employee, 1L, request))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("許可されていません");
    }

    @Test
    @DisplayName("update: 休憩時刻の修正")
    void update_breakModification_updatesBreaks() {
        var existingBreak = BreakRecord.builder()
                .id(1L)
                .breakStart(LocalTime.of(12, 0))
                .breakEnd(LocalTime.of(13, 0))
                .build();
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .clockOut(LocalTime.of(18, 0))
                .totalWorkMinutes(480)
                .totalBreakMinutes(60)
                .overtimeMinutes(0)
                .breakRecords(new ArrayList<>(List.of(existingBreak)))
                .build();
        var request = new AttendanceUpdateRequest(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                List.of(new BreakUpdateRequest(1L, LocalTime.of(12, 30), LocalTime.of(13, 30)))
        );

        when(attendanceRecordRepository.findById(1L)).thenReturn(Optional.of(record));
        when(breakRecordRepository.findById(1L)).thenReturn(Optional.of(existingBreak));
        when(breakRecordRepository.save(any(BreakRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateAttendance(employee, 1L, request);

        assertThat(result.totalBreakMinutes()).isEqualTo(60);
        verify(revisionRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("getMyAttendance: 月の全日分を返す")
    void getMyAttendance_returnsAllDays() {
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.of(2026, 7, 1))
                .clockIn(LocalTime.of(9, 0))
                .clockOut(LocalTime.of(18, 0))
                .totalWorkMinutes(480)
                .totalBreakMinutes(60)
                .overtimeMinutes(0)
                .breakRecords(new ArrayList<>())
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDateBetween(
                1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(List.of(record));

        var result = service.getMyAttendance(employee, 2026, 7);

        assertThat(result).hasSize(31);
        assertThat(result.getFirst().clockIn()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.get(1).clockIn()).isNull();
    }

    @Test
    @DisplayName("コアタイムチェック: 正常勤務 → OK")
    void clockOut_normalHours_statusOK() {
        var request = new ClockOutRequest(LocalTime.of(18, 0));
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(9, 0))
                .breakRecords(new ArrayList<>())
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.clockOut(employee, request);

        assertThat(result.status()).isEqualTo(AttendanceStatus.OK);
    }

    @Test
    @DisplayName("コアタイムチェック: 遅刻 → LATE_START")
    void clockOut_lateStart_statusLateStart() {
        var request = new ClockOutRequest(LocalTime.of(19, 30));
        var record = AttendanceRecord.builder()
                .id(1L)
                .employee(employee)
                .date(LocalDate.now())
                .clockIn(LocalTime.of(10, 30))
                .breakRecords(new ArrayList<>())
                .build();
        when(attendanceRecordRepository.findByEmployeeIdAndDate(1L, LocalDate.now()))
                .thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.clockOut(employee, request);

        assertThat(result.status()).isEqualTo(AttendanceStatus.LATE_START);
    }
}
