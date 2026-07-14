package com.example.attendance.application.service;

import com.example.attendance.application.dto.AttendanceDetailResponse;
import com.example.attendance.application.dto.AttendanceResponse;
import com.example.attendance.application.dto.AttendanceUpdateRequest;
import com.example.attendance.application.dto.BreakEndRequest;
import com.example.attendance.application.dto.BreakStartRequest;
import com.example.attendance.application.dto.BreakUpdateRequest;
import com.example.attendance.application.dto.ClockInRequest;
import com.example.attendance.application.dto.ClockOutRequest;
import com.example.attendance.domain.model.AttendanceRecord;
import com.example.attendance.domain.model.AttendanceRevision;
import com.example.attendance.domain.model.BreakRecord;
import com.example.attendance.domain.model.Employee;
import com.example.attendance.domain.model.WorkDuration;
import com.example.attendance.domain.repository.AttendanceRecordRepository;
import com.example.attendance.domain.repository.AttendanceRevisionRepository;
import com.example.attendance.domain.repository.BreakRecordRepository;
import com.example.attendance.infrastructure.exception.AttendanceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final BreakRecordRepository breakRecordRepository;
    private final AttendanceRevisionRepository revisionRepository;

    public AttendanceServiceImpl(
            AttendanceRecordRepository attendanceRecordRepository,
            BreakRecordRepository breakRecordRepository,
            AttendanceRevisionRepository revisionRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.breakRecordRepository = breakRecordRepository;
        this.revisionRepository = revisionRepository;
    }

    @Override
    public AttendanceResponse clockIn(Employee employee, ClockInRequest request) {
        var today = LocalDate.now();
        var existing = attendanceRecordRepository.findByEmployeeIdAndDate(employee.getId(), today);

        if (existing.isPresent()) {
            throw AttendanceException.alreadyClockedIn();
        }

        var record = AttendanceRecord.builder()
                .employee(employee)
                .date(today)
                .clockIn(request.clockIn())
                .breakRecords(new ArrayList<>())
                .build();

        var saved = attendanceRecordRepository.save(record);
        return AttendanceResponse.from(saved);
    }

    @Override
    public AttendanceResponse clockOut(Employee employee, ClockOutRequest request) {
        var today = LocalDate.now();
        var record = attendanceRecordRepository.findByEmployeeIdAndDate(employee.getId(), today)
                .orElseThrow(AttendanceException::notClockedIn);

        if (record.getClockIn() == null) {
            throw AttendanceException.notClockedIn();
        }

        if (hasUnfinishedBreak(record)) {
            throw AttendanceException.breakNotEnded();
        }

        record.setClockOut(request.clockOut());
        recalculateWorkDuration(record);

        var saved = attendanceRecordRepository.save(record);
        return AttendanceResponse.from(saved);
    }

    @Override
    public AttendanceResponse breakStart(Employee employee, BreakStartRequest request) {
        var today = LocalDate.now();
        var record = attendanceRecordRepository.findByEmployeeIdAndDate(employee.getId(), today)
                .orElseThrow(AttendanceException::notClockedIn);

        if (record.getClockIn() == null) {
            throw AttendanceException.notClockedIn();
        }

        if (hasUnfinishedBreak(record)) {
            throw AttendanceException.breakNotEnded();
        }

        var breakRecord = BreakRecord.builder()
                .attendanceRecord(record)
                .breakStart(request.breakStart())
                .build();
        breakRecordRepository.save(breakRecord);
        record.getBreakRecords().add(breakRecord);

        var saved = attendanceRecordRepository.save(record);
        return AttendanceResponse.from(saved);
    }

    @Override
    public AttendanceResponse breakEnd(Employee employee, BreakEndRequest request) {
        var today = LocalDate.now();
        var record = attendanceRecordRepository.findByEmployeeIdAndDate(employee.getId(), today)
                .orElseThrow(AttendanceException::notClockedIn);

        var activeBreak = record.getBreakRecords().stream()
                .filter(b -> b.getBreakEnd() == null)
                .findFirst()
                .orElseThrow(AttendanceException::noActiveBreak);

        activeBreak.setBreakEnd(request.breakEnd());
        breakRecordRepository.save(activeBreak);

        var saved = attendanceRecordRepository.save(record);
        return AttendanceResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getToday(Employee employee) {
        var today = LocalDate.now();
        return attendanceRecordRepository.findByEmployeeIdAndDate(employee.getId(), today)
                .map(AttendanceResponse::from)
                .orElse(AttendanceResponse.empty(today));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDetailResponse> getMyAttendance(Employee employee, int year, int month) {
        var yearMonth = YearMonth.of(year, month);
        var startDate = yearMonth.atDay(1);
        var endDate = yearMonth.atEndOfMonth();

        var records = attendanceRecordRepository.findByEmployeeIdAndDateBetween(
                employee.getId(), startDate, endDate);

        Map<LocalDate, AttendanceRecord> recordMap = records.stream()
                .collect(Collectors.toMap(AttendanceRecord::getDate, Function.identity()));

        var departmentName = employee.getDepartment() != null ? employee.getDepartment().getName() : null;

        List<AttendanceDetailResponse> result = new ArrayList<>();
        for (var date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            var record = recordMap.get(date);
            if (record != null) {
                result.add(AttendanceDetailResponse.from(record));
            } else {
                result.add(AttendanceDetailResponse.empty(date, employee.getId(), employee.getName(), departmentName));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDetailResponse> getAllAttendance(int year, int month, Long departmentId, String employeeName) {
        var yearMonth = YearMonth.of(year, month);
        var startDate = yearMonth.atDay(1);
        var endDate = yearMonth.atEndOfMonth();

        var records = attendanceRecordRepository.findAllByFilters(startDate, endDate, departmentId, employeeName);
        return records.stream().map(AttendanceDetailResponse::from).toList();
    }

    @Override
    public AttendanceResponse updateAttendance(Employee employee, Long id, AttendanceUpdateRequest request) {
        var record = attendanceRecordRepository.findById(id)
                .orElseThrow(AttendanceException::notFound);

        if (!record.getEmployee().getId().equals(employee.getId())) {
            throw AttendanceException.forbidden();
        }

        var now = YearMonth.now();
        var recordMonth = YearMonth.from(record.getDate());
        if (!recordMonth.equals(now)) {
            throw AttendanceException.notCurrentMonth();
        }

        updateClockTimes(record, request, employee);
        updateBreaks(record, request.breaks(), employee);
        recalculateWorkDuration(record);

        var saved = attendanceRecordRepository.save(record);
        return AttendanceResponse.from(saved);
    }

    private void updateClockTimes(AttendanceRecord record, AttendanceUpdateRequest request, Employee employee) {
        if (!Objects.equals(record.getClockIn(), request.clockIn())) {
            createRevision(record, employee, "clockIn",
                    record.getClockIn() != null ? record.getClockIn().toString() : null,
                    request.clockIn() != null ? request.clockIn().toString() : null);
            record.setClockIn(request.clockIn());
        }

        if (!Objects.equals(record.getClockOut(), request.clockOut())) {
            createRevision(record, employee, "clockOut",
                    record.getClockOut() != null ? record.getClockOut().toString() : null,
                    request.clockOut() != null ? request.clockOut().toString() : null);
            record.setClockOut(request.clockOut());
        }
    }

    private void updateBreaks(AttendanceRecord record, List<BreakUpdateRequest> breakRequests, Employee employee) {
        if (breakRequests == null) {
            return;
        }

        var existingBreaks = record.getBreakRecords();
        var requestedIds = breakRequests.stream()
                .map(BreakUpdateRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Delete breaks not in the request
        var toDelete = existingBreaks.stream()
                .filter(b -> !requestedIds.contains(b.getId()))
                .toList();
        for (var breakToDelete : toDelete) {
            createRevision(record, employee, "breakStart:" + breakToDelete.getId(),
                    breakToDelete.getBreakStart().toString(), null);
            breakRecordRepository.delete(breakToDelete);
        }
        existingBreaks.removeAll(toDelete);

        // Update existing or create new
        for (var breakReq : breakRequests) {
            if (breakReq.id() != null) {
                var existing = breakRecordRepository.findById(breakReq.id()).orElse(null);
                if (existing != null) {
                    if (!Objects.equals(existing.getBreakStart(), breakReq.breakStart())) {
                        createRevision(record, employee, "breakStart:" + existing.getId(),
                                existing.getBreakStart().toString(),
                                breakReq.breakStart() != null ? breakReq.breakStart().toString() : null);
                        existing.setBreakStart(breakReq.breakStart());
                    }
                    if (!Objects.equals(existing.getBreakEnd(), breakReq.breakEnd())) {
                        createRevision(record, employee, "breakEnd:" + existing.getId(),
                                existing.getBreakEnd() != null ? existing.getBreakEnd().toString() : null,
                                breakReq.breakEnd() != null ? breakReq.breakEnd().toString() : null);
                        existing.setBreakEnd(breakReq.breakEnd());
                    }
                    breakRecordRepository.save(existing);
                }
            } else {
                var newBreak = BreakRecord.builder()
                        .attendanceRecord(record)
                        .breakStart(breakReq.breakStart())
                        .breakEnd(breakReq.breakEnd())
                        .build();
                var saved = breakRecordRepository.save(newBreak);
                existingBreaks.add(saved);
            }
        }
    }

    private void recalculateWorkDuration(AttendanceRecord record) {
        var duration = WorkDuration.calculate(record.getClockIn(), record.getClockOut(), record.getBreakRecords());
        record.setTotalWorkMinutes(duration.totalWorkMinutes());
        record.setTotalBreakMinutes(duration.totalBreakMinutes());
        record.setOvertimeMinutes(duration.overtimeMinutes());
    }

    private void createRevision(AttendanceRecord record, Employee employee, String fieldName, String oldValue, String newValue) {
        var revision = AttendanceRevision.builder()
                .attendanceRecord(record)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .revisedBy(employee)
                .build();
        revisionRepository.save(revision);
    }

    private boolean hasUnfinishedBreak(AttendanceRecord record) {
        return record.getBreakRecords().stream().anyMatch(b -> b.getBreakEnd() == null);
    }
}
