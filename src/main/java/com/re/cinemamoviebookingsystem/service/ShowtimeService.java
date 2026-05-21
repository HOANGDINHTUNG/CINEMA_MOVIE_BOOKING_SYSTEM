package com.re.cinemamoviebookingsystem.service;

import com.re.cinemamoviebookingsystem.config.CinemaProperties;
import com.re.cinemamoviebookingsystem.dto.request.ShowtimeBulkCreateRequest;
import com.re.cinemamoviebookingsystem.dto.request.ShowtimeCreateRequest;
import com.re.cinemamoviebookingsystem.dto.request.ShowtimeUpdateRequest;
import com.re.cinemamoviebookingsystem.dto.response.*;
import com.re.cinemamoviebookingsystem.tmdb.enums.AppLanguage;
import com.re.cinemamoviebookingsystem.util.MovieAgeUtil;
import com.re.cinemamoviebookingsystem.entity.*;
import com.re.cinemamoviebookingsystem.enums.SeatStatus;
import com.re.cinemamoviebookingsystem.enums.ShowtimeStatus;
import com.re.cinemamoviebookingsystem.exception.BusinessException;
import com.re.cinemamoviebookingsystem.exception.ErrorCode;
import com.re.cinemamoviebookingsystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final ShowtimeStatusService showtimeStatusService;
    private final CinemaProperties cinemaProperties;
    private final MovieDisplayService movieDisplayService;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;
    private final ShowtimeSeatRepairService showtimeSeatRepairService;
    private final SeatMapService seatMapService;

    private static final DateTimeFormatter TAB_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter SLOT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @Transactional(rollbackFor = Exception.class)
    public Long createShowtime(ShowtimeCreateRequest request) {
        Movie movie = resolveMovie(request);
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phòng không tồn tại"));

        LocalDateTime start = request.getStartTime();
        if (start == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Chọn giờ bắt đầu");
        }
        if (!start.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Giờ bắt đầu phải sau thời điểm hiện tại");
        }
        LocalDateTime end = start.plusMinutes(movie.getDuration())
                .plusMinutes(cinemaProperties.getCleaningBufferMinutes());

        assertNoRoomConflict(room, start, end, null);

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .room(room)
                .startTime(start)
                .endTime(end)
                .basePrice(request.getBasePrice())
                .status(ShowtimeStatus.ACTIVE)
                .build();
        showtime = showtimeRepository.save(showtime);

        List<Seat> seats = seatRepository.findByRoomRoomId(room.getRoomId());
        List<ShowtimeSeat> showtimeSeats = new ArrayList<>();
        for (Seat seat : seats) {
            showtimeSeats.add(ShowtimeSeat.builder()
                    .showtime(showtime)
                    .seat(seat)
                    .status(SeatStatus.AVAILABLE)
                    .build());
        }
        showtimeSeatRepository.saveAll(showtimeSeats);
        return showtime.getShowtimeId();
    }

    @Transactional(readOnly = true)
    public ScheduleViewDto buildScheduleView(LocalDate selectedDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        List<ShowtimeBrowseDto> window = browseScheduleWindow();

        TreeSet<LocalDate> datesWithShowtimes = window.stream()
                .map(s -> s.getStartTime().toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));

        List<LocalDate> dayList = buildScheduleDayTabs(datesWithShowtimes, today);

        LocalDate selected = resolveSelectedScheduleDate(selectedDate, dayList, datesWithShowtimes, today, window, now);

        Map<Long, List<ShowtimeBrowseDto>> grouped = window.stream()
                .filter(s -> s.getTmdbId() != null)
                .filter(s -> s.getStartTime().toLocalDate().equals(selected))
                .filter(s -> s.getStartTime().isAfter(now))
                .collect(Collectors.groupingBy(ShowtimeBrowseDto::getTmdbId));

        List<ScheduleDayTabDto> tabs = dayList.stream()
                .map(d -> ScheduleDayTabDto.builder()
                        .date(d)
                        .label(d.format(TAB_DATE))
                        .selected(d.equals(selected))
                        .build())
                .toList();

        List<ScheduleMovieCardDto> cards = new ArrayList<>();
        for (Map.Entry<Long, List<ShowtimeBrowseDto>> entry : grouped.entrySet()) {
            Long tmdbId = entry.getKey();
            ShowtimeBrowseDto first = entry.getValue().get(0);
            List<ScheduleSlotDto> slotDtos = entry.getValue().stream()
                    .sorted(Comparator.comparing(ShowtimeBrowseDto::getStartTime))
                    .map(s -> ScheduleSlotDto.builder()
                            .showtimeId(s.getShowtimeId())
                            .timeLabel(s.getStartTime().format(SLOT_TIME))
                            .soldOut(s.isSoldOut())
                            .build())
                    .toList();
            String ageLabel = resolveAgeLabel(first);
            String displayTitle = MovieAgeUtil.appendAgeSuffixToTitle(first.getMovieTitle(), ageLabel);
            cards.add(ScheduleMovieCardDto.builder()
                    .tmdbId(tmdbId)
                    .movieId(first.getMovieId())
                    .title(displayTitle)
                    .posterUrl(null)
                    .genresLabel("—")
                    .duration(null)
                    .releaseDate(null)
                    .ageLabel(ageLabel)
                    .ageNote(MovieAgeUtil.buildAgeNote(first.getMovieTitle(), first.getAgeLabel()))
                    .format("2D")
                    .slots(slotDtos)
                    .build());
        }
        cards.sort(Comparator.comparing(ScheduleMovieCardDto::getTitle, String.CASE_INSENSITIVE_ORDER));

        return ScheduleViewDto.builder()
                .days(tabs)
                .selectedDate(selected)
                .movies(cards)
                .filtering(false)
                .build();
    }

    private List<LocalDate> buildScheduleDayTabs(TreeSet<LocalDate> datesWithShowtimes, LocalDate today) {
        if (datesWithShowtimes.isEmpty()) {
            return IntStream.range(0, 5).mapToObj(today::plusDays).toList();
        }
        List<LocalDate> tabs = new ArrayList<>(datesWithShowtimes.stream().limit(7).toList());
        if (tabs.size() < 5) {
            LocalDate cursor = tabs.get(tabs.size() - 1);
            while (tabs.size() < 5) {
                cursor = cursor.plusDays(1);
                tabs.add(cursor);
            }
        }
        return tabs;
    }

    private static LocalDate resolveSelectedScheduleDate(LocalDate selectedDate,
                                                         List<LocalDate> dayList,
                                                         TreeSet<LocalDate> datesWithShowtimes,
                                                         LocalDate today,
                                                         List<ShowtimeBrowseDto> window,
                                                         LocalDateTime now) {
        if (selectedDate != null && dayList.contains(selectedDate)
                && hasBookableShowtimesOnDay(window, selectedDate, now)) {
            return selectedDate;
        }
        for (LocalDate day : dayList) {
            if (hasBookableShowtimesOnDay(window, day, now)) {
                return day;
            }
        }
        if (selectedDate != null && dayList.contains(selectedDate)) {
            return selectedDate;
        }
        if (!datesWithShowtimes.isEmpty()) {
            return datesWithShowtimes.first();
        }
        return dayList.isEmpty() ? today : dayList.get(0);
    }

    private static boolean hasBookableShowtimesOnDay(List<ShowtimeBrowseDto> window,
                                                       LocalDate day,
                                                       LocalDateTime now) {
        return window.stream()
                .anyMatch(s -> s.getStartTime().toLocalDate().equals(day) && s.getStartTime().isAfter(now));
    }

    @Transactional(readOnly = true)
    public List<ShowtimeBrowseDto> browseScheduleWindow() {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDate.now().plusDays(60).atTime(23, 59, 59);
        List<ShowtimeStatus> statuses = List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);
        return showtimeRepository.findScheduleWindow(from, to, statuses).stream()
                .map(this::toBrowseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<Long, LocalDateTime> mapNextShowtimeByTmdbId() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeStatus> statuses = List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);
        Map<Long, LocalDateTime> nextByTmdb = new LinkedHashMap<>();
        for (Showtime showtime : showtimeRepository.findUpcoming(now, statuses)) {
            Long tmdbId = showtime.getMovie().getTmdbId();
            if (tmdbId == null) {
                continue;
            }
            LocalDateTime start = showtime.getStartTime();
            nextByTmdb.merge(tmdbId, start, (a, b) -> a.isBefore(b) ? a : b);
        }
        return nextByTmdb;
    }

    @Transactional(readOnly = true)
    public List<ShowtimeBrowseDto> browseUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeStatus> statuses = List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);
        return showtimeRepository.findUpcoming(now, statuses).stream()
                .map(this::toBrowseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Showtime> listAllForAdmin() {
        return showtimeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<AdminShowtimeListItemDto> listForAdmin(Long movieId, Integer roomId, ShowtimeStatus status,
                                                       LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return showtimeRepository.findForAdmin(movieId, roomId, status, from, to, pageable)
                .map(this::toAdminListItem);
    }

    @Transactional(readOnly = true)
    public AdminShowtimeDetailDto getAdminDetail(Long showtimeId) {
        Showtime showtime = showtimeRepository.findByIdWithDetails(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND, "Suất chiếu không tồn tại"));
        showtimeStatusService.refreshShowtimeStatus(showtimeId);
        showtime = showtimeRepository.findByIdWithDetails(showtimeId).orElseThrow();

        long booked = showtimeSeatRepository.countByShowtimeShowtimeIdAndStatus(
                showtimeId, SeatStatus.BOOKED);
        long locked = showtimeSeatRepository.countByShowtimeShowtimeIdAndStatus(
                showtimeId, SeatStatus.LOCKED);
        int total = showtime.getRoom().getTotalSeats();
        long available = Math.max(0, total - booked - locked);
        long paidBookings = bookingRepository.countByShowtimeShowtimeIdAndStatus(
                showtimeId, com.re.cinemamoviebookingsystem.enums.BookingStatus.PAID);
        long seatRows = showtimeSeatRepository.countByShowtimeShowtimeId(showtimeId);

        boolean canEdit = showtime.getStatus() != ShowtimeStatus.CANCELLED && booked == 0;
        boolean canCancel = showtime.getStatus() != ShowtimeStatus.CANCELLED && paidBookings == 0;

        return AdminShowtimeDetailDto.builder()
                .showtimeId(showtime.getShowtimeId())
                .movieId(showtime.getMovie().getMovieId())
                .tmdbId(showtime.getMovie().getTmdbId())
                .movieTitle(movieDisplayService.resolveTitleLocal(showtime.getMovie()))
                .roomId(showtime.getRoom().getRoomId())
                .roomName(showtime.getRoom().getRoomName())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .status(showtime.getStatus())
                .availableSeats(available)
                .lockedSeats(locked)
                .bookedSeats(booked)
                .totalSeats(total)
                .paidBookings(paidBookings)
                .canEdit(canEdit)
                .canCancel(canCancel)
                .showtimeSeatRows(seatRows)
                .seatsConfigured(seatRows > 0)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateShowtime(Long showtimeId, ShowtimeUpdateRequest request) {
        Showtime showtime = showtimeRepository.findByIdWithDetails(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND, "Suất chiếu không tồn tại"));
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Suất đã hủy");
        }
        long booked = showtimeSeatRepository.countByShowtimeShowtimeIdAndStatus(
                showtimeId, SeatStatus.BOOKED);
        if (booked > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Không sửa được suất đã có ghế đặt. Chỉ có thể hủy suất.");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phòng không tồn tại"));
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = start.plusMinutes(showtime.getMovie().getDuration())
                .plusMinutes(cinemaProperties.getCleaningBufferMinutes());

        assertNoRoomConflict(room, start, end, showtimeId);

        if (!room.getRoomId().equals(showtime.getRoom().getRoomId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Không đổi phòng khi suất đã tạo ghế. Tạo suất mới thay thế.");
        }

        showtime.setStartTime(start);
        showtime.setEndTime(end);
        showtime.setBasePrice(request.getBasePrice());
        showtime.setRoom(room);
        showtimeRepository.save(showtime);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelShowtime(Long showtimeId) {
        Showtime showtime = showtimeRepository.findByIdWithDetails(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND, "Suất chiếu không tồn tại"));
        long paid = bookingRepository.countByShowtimeShowtimeIdAndStatus(
                showtimeId, com.re.cinemamoviebookingsystem.enums.BookingStatus.PAID);
        if (paid > 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Không hủy suất đã có đơn thanh toán");
        }
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        showtimeRepository.save(showtime);
        auditLogService.log("SHOWTIME_CANCEL", "SHOWTIME", String.valueOf(showtimeId), null);
    }

    @Transactional(readOnly = true)
    public boolean checkRoomConflict(Integer roomId, LocalDateTime start, Long excludeShowtimeId, int durationMinutes) {
        return checkRoomConflictDetail(roomId, start, excludeShowtimeId, durationMinutes).isConflict();
    }

    @Transactional(readOnly = true)
    public ShowtimeConflictCheckDto checkRoomConflictDetail(Integer roomId,
                                                            LocalDateTime start,
                                                            Long excludeShowtimeId,
                                                            int durationMinutes) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phòng không tồn tại"));
        LocalDateTime end = computeBlockedEnd(start, durationMinutes);
        List<Showtime> overlapping = showtimeRepository.findOverlappingInRoom(
                roomId, start, end, excludeShowtimeId);
        if (overlapping.isEmpty()) {
            return ShowtimeConflictCheckDto.builder()
                    .conflict(false)
                    .message("Khung giờ hợp lệ — có thể đặt suất chiếu.")
                    .proposedStart(start)
                    .proposedEnd(end)
                    .cleaningBufferMinutes(cinemaProperties.getCleaningBufferMinutes())
                    .conflicts(List.of())
                    .build();
        }
        List<ShowtimeConflictCheckDto.ConflictingShowtimeDto> conflictItems = overlapping.stream()
                .map(s -> ShowtimeConflictCheckDto.ConflictingShowtimeDto.builder()
                        .showtimeId(s.getShowtimeId())
                        .movieTitle(movieDisplayService.resolveTitleLocal(s.getMovie()))
                        .roomName(s.getRoom().getRoomName())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .build())
                .toList();
        LocalDateTime suggested = overlapping.stream()
                .map(Showtime::getEndTime)
                .max(LocalDateTime::compareTo)
                .orElse(end);
        String message = buildConflictMessage(room.getRoomName(), start, end, overlapping, suggested);
        return ShowtimeConflictCheckDto.builder()
                .conflict(true)
                .message(message)
                .proposedStart(start)
                .proposedEnd(end)
                .suggestedStart(suggested)
                .cleaningBufferMinutes(cinemaProperties.getCleaningBufferMinutes())
                .conflicts(conflictItems)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public ShowtimeBulkCreateResultDto bulkCreate(ShowtimeBulkCreateRequest request) {
        Movie movie = resolveMovieForBulk(request);
        List<java.time.LocalTime> slots = parseTimeSlots(request.getTimeSlots());
        if (slots.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cần ít nhất một khung giờ");
        }
        validateBulkDateAndTimes(request, slots);
        int created = 0;
        List<String> conflictMessages = new ArrayList<>();
        for (LocalDate day = request.getStartDate(); !day.isAfter(request.getEndDate()); day = day.plusDays(1)) {
            for (java.time.LocalTime slot : slots) {
                ShowtimeCreateRequest single = new ShowtimeCreateRequest();
                single.setTmdbId(movie.getTmdbId());
                single.setMovieId(movie.getMovieId());
                single.setRoomId(request.getRoomId());
                single.setStartTime(day.atTime(slot));
                single.setBasePrice(request.getBasePrice());
                try {
                    createShowtime(single);
                    created++;
                } catch (BusinessException ex) {
                    if (ex.getErrorCode() != ErrorCode.ROOM_CONFLICT) {
                        throw ex;
                    }
                    conflictMessages.add(day + " " + slot + ": " + ex.getMessage());
                }
            }
        }
        return ShowtimeBulkCreateResultDto.builder()
                .created(created)
                .skippedConflicts(conflictMessages.size())
                .conflictMessages(conflictMessages)
                .build();
    }

    private LocalDateTime computeBlockedEnd(LocalDateTime start, int durationMinutes) {
        return start.plusMinutes(durationMinutes)
                .plusMinutes(cinemaProperties.getCleaningBufferMinutes());
    }

    private void assertNoRoomConflict(Room room, LocalDateTime start, LocalDateTime end, Long excludeShowtimeId) {
        List<Showtime> overlapping = showtimeRepository.findOverlappingInRoom(
                room.getRoomId(), start, end, excludeShowtimeId);
        if (overlapping.isEmpty()) {
            return;
        }
        LocalDateTime suggested = overlapping.stream()
                .map(Showtime::getEndTime)
                .max(LocalDateTime::compareTo)
                .orElse(end);
        throw new BusinessException(ErrorCode.ROOM_CONFLICT,
                buildConflictMessage(room.getRoomName(), start, end, overlapping, suggested));
    }

    private String buildConflictMessage(String roomName,
                                        LocalDateTime proposedStart,
                                        LocalDateTime proposedEnd,
                                        List<Showtime> overlapping,
                                        LocalDateTime suggestedNextStart) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Showtime first = overlapping.get(0);
        String title = movieDisplayService.resolveTitleLocal(first.getMovie());
        StringBuilder sb = new StringBuilder();
        sb.append("Phòng ").append(roomName).append(" không đủ khoảng trống. ");
        sb.append("Suất đề xuất ").append(proposedStart.format(fmt))
                .append("–").append(proposedEnd.format(fmt));
        sb.append(" (đã gồm ").append(cinemaProperties.getCleaningBufferMinutes())
                .append(" phút dọn phòng sau phim). ");
        sb.append("Trùng suất #").append(first.getShowtimeId()).append(" «")
                .append(title).append("» ")
                .append(first.getStartTime().format(fmt))
                .append("–").append(first.getEndTime().format(fmt));
        if (overlapping.size() > 1) {
            sb.append(" (+").append(overlapping.size() - 1).append(" suất khác)");
        }
        sb.append(". Gợi ý: bắt đầu từ ").append(suggestedNextStart.format(fmt));
        return sb.toString();
    }

    private Movie resolveMovieForBulk(ShowtimeBulkCreateRequest request) {
        ShowtimeCreateRequest tmp = new ShowtimeCreateRequest();
        tmp.setMovieId(request.getMovieId());
        tmp.setTmdbId(request.getTmdbId());
        return resolveMovie(tmp);
    }

    /**
     * Kiểm tra khoảng ngày lọc admin: đến ngày ≥ từ ngày.
     */
    public void validateAdminFilterDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && toDate.isBefore(fromDate)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Đến ngày phải sau hoặc bằng Từ ngày");
        }
    }

    private void validateBulkDateAndTimes(ShowtimeBulkCreateRequest request, List<java.time.LocalTime> slots) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (start == null || end == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Chọn đủ Từ ngày và Đến ngày");
        }
        if (start.isBefore(today)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Từ ngày không được trước hôm nay (" + today.format(TAB_DATE) + ")");
        }
        if (end.isBefore(today)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Đến ngày không được trước hôm nay");
        }
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Đến ngày phải sau hoặc bằng Từ ngày");
        }

        List<String> pastSlots = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            for (java.time.LocalTime slot : slots) {
                LocalDateTime showStart = day.atTime(slot);
                if (!showStart.isAfter(now)) {
                    pastSlots.add(day.format(TAB_DATE) + " " + slot.format(SLOT_TIME));
                }
            }
        }
        if (!pastSlots.isEmpty()) {
            String preview = pastSlots.stream().limit(6).reduce((a, b) -> a + ", " + b).orElse("");
            String suffix = pastSlots.size() > 6 ? " …" : "";
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "Có khung giờ đã qua (không tạo suất trong quá khứ): " + preview + suffix
                            + ". Chỉ chọn ngày và khung giờ từ hiện tại trở đi.");
        }
    }

    private List<java.time.LocalTime> parseTimeSlots(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of(
                    java.time.LocalTime.of(10, 0),
                    java.time.LocalTime.of(14, 0),
                    java.time.LocalTime.of(18, 0),
                    java.time.LocalTime.of(21, 30));
        }
        List<java.time.LocalTime> result = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            if (part.isBlank()) continue;
            String token = part.trim();
            try {
                if (token.length() == 4 && token.indexOf(':') < 0) {
                    token = token.substring(0, 2) + ":" + token.substring(2);
                }
                result.add(java.time.LocalTime.parse(token));
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST,
                        "Khung giờ không hợp lệ: «" + part.trim() + "». Dùng định dạng HH:mm, cách nhau dấu phẩy.");
            }
        }
        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cần ít nhất một khung giờ hợp lệ");
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ShowtimeCalendarViewDto buildCalendarView(LocalDate weekAnchor, Integer roomId) {
        LocalDate anchor = weekAnchor != null ? weekAnchor : LocalDate.now();
        LocalDate weekStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekEnd.plusDays(1).atStartOfDay();

        List<Room> rooms = roomId != null
                ? roomRepository.findById(roomId).map(List::of).orElse(List.of())
                : roomRepository.findAll();

        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(weekStart.plusDays(i));
        }

        Map<Integer, Map<String, List<ShowtimeCalendarEventDto>>> grid = new LinkedHashMap<>();
        for (Room room : rooms) {
            Map<String, List<ShowtimeCalendarEventDto>> byDay = new LinkedHashMap<>();
            for (LocalDate d : days) {
                byDay.put(d.toString(), new ArrayList<>());
            }
            grid.put(room.getRoomId(), byDay);
        }

        for (Showtime s : showtimeRepository.findForCalendar(from, to, roomId)) {
            String dayKey = s.getStartTime().toLocalDate().toString();
            Map<String, List<ShowtimeCalendarEventDto>> byDay = grid.get(s.getRoom().getRoomId());
            if (byDay != null && byDay.containsKey(dayKey)) {
                byDay.get(dayKey).add(toCalendarEvent(s));
            }
        }
        for (Map<String, List<ShowtimeCalendarEventDto>> byDay : grid.values()) {
            for (List<ShowtimeCalendarEventDto> events : byDay.values()) {
                events.sort(Comparator.comparing(ShowtimeCalendarEventDto::getStartTime));
            }
        }

        List<ShowtimeCalendarViewDto.RoomColumnDto> roomColumns = rooms.stream()
                .map(r -> ShowtimeCalendarViewDto.RoomColumnDto.builder()
                        .roomId(r.getRoomId())
                        .roomName(r.getRoomName())
                        .build())
                .collect(Collectors.toList());

        return ShowtimeCalendarViewDto.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .previousWeek(weekStart.minusWeeks(1))
                .nextWeek(weekStart.plusWeeks(1))
                .roomFilter(roomId)
                .rooms(roomColumns)
                .days(days)
                .grid(grid)
                .build();
    }

    private ShowtimeCalendarEventDto toCalendarEvent(Showtime s) {
        long booked = showtimeSeatRepository.countByShowtimeShowtimeIdAndStatus(
                s.getShowtimeId(), SeatStatus.BOOKED);
        int total = s.getRoom().getTotalSeats();
        int fill = total > 0 ? (int) (booked * 100 / total) : 0;
        return ShowtimeCalendarEventDto.builder()
                .showtimeId(s.getShowtimeId())
                .roomId(s.getRoom().getRoomId())
                .roomName(s.getRoom().getRoomName())
                .movieTitle(movieDisplayService.resolveTitleLocal(s.getMovie()))
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .status(s.getStatus())
                .fillPercent(fill)
                .build();
    }

    private AdminShowtimeListItemDto toAdminListItem(Showtime s) {
        long booked = showtimeSeatRepository.countByShowtimeShowtimeIdAndStatus(
                s.getShowtimeId(), SeatStatus.BOOKED);
        int total = s.getRoom().getTotalSeats();
        int fill = total > 0 ? (int) (booked * 100 / total) : 0;
        Movie movie = s.getMovie();
        return AdminShowtimeListItemDto.builder()
                .showtimeId(s.getShowtimeId())
                .movieId(movie.getMovieId())
                .tmdbId(movie.getTmdbId())
                .movieTitle(movieDisplayService.resolveTitleLocal(movie))
                .roomName(s.getRoom().getRoomName())
                .roomId(s.getRoom().getRoomId())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .basePrice(s.getBasePrice())
                .status(s.getStatus())
                .bookedSeats(booked)
                .totalSeats(total)
                .fillPercent(fill)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AdminShowtimeListItemDto> listByMovieForAdmin(Long movieId) {
        return showtimeRepository.findByMovieIdWithDetails(movieId).stream()
                .map(this::toAdminListItem)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowtimeBrowseDto> listByMovie(Long movieId) {
        LocalDateTime now = LocalDateTime.now();
        return showtimeRepository.findUpcoming(now, List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT))
                .stream()
                .filter(s -> s.getMovie().getMovieId().equals(movieId))
                .map(this::toBrowseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowtimeBrowseDto> listByTmdbId(long tmdbId) {
        LocalDateTime now = LocalDateTime.now();
        List<ShowtimeStatus> statuses = List.of(ShowtimeStatus.ACTIVE, ShowtimeStatus.SOLD_OUT);
        List<Showtime> showtimes = showtimeRepository.findUpcomingByTmdbId(tmdbId, now, statuses);
        if (showtimes.isEmpty()) {
            return List.of();
        }

        // Resolve movie title once per movie instead of once per showtime.
        String movieTitle = movieDisplayService.resolveTitleLocal(showtimes.get(0).getMovie());
        return showtimes.stream()
                .map(s -> toBrowseDto(s, movieTitle))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowtimeDayGroupDto> groupShowtimesByDay(List<ShowtimeBrowseDto> showtimes, Locale locale) {
        if (showtimes == null || showtimes.isEmpty()) {
            return List.of();
        }
        Locale effective = locale != null ? locale : Locale.forLanguageTag("vi-VN");
        DateTimeFormatter dayLabel = DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", effective);
        Map<LocalDate, List<ShowtimeBrowseDto>> grouped = showtimes.stream()
                .collect(Collectors.groupingBy(s -> s.getStartTime().toLocalDate(), TreeMap::new, Collectors.toList()));
        List<ShowtimeDayGroupDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<ShowtimeBrowseDto>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<ShowtimeBrowseDto> slots = entry.getValue().stream()
                    .sorted(Comparator.comparing(ShowtimeBrowseDto::getStartTime))
                    .collect(Collectors.toList());
            result.add(ShowtimeDayGroupDto.builder()
                    .date(date)
                    .dateLabel(date.format(dayLabel))
                    .monthLabel(formatScheduleMonth(date, effective))
                    .dayNumber(String.valueOf(date.getDayOfMonth()))
                    .weekdayLabel(formatScheduleWeekday(date, effective))
                    .slots(slots)
                    .build());
        }
        return result;
    }

    private static String formatScheduleMonth(LocalDate date, Locale locale) {
        if ("vi".equalsIgnoreCase(locale.getLanguage())) {
            return "Th. " + date.format(DateTimeFormatter.ofPattern("MM"));
        }
        return date.format(DateTimeFormatter.ofPattern("MMM", locale));
    }

    private static String formatScheduleWeekday(LocalDate date, Locale locale) {
        return date.format(DateTimeFormatter.ofPattern("EEEE", locale));
    }

    @Transactional(readOnly = true)
    public SeatMapDto getSeatMap(Long showtimeId) {
        return seatMapService.getSeatMap(showtimeId);
    }

    @Transactional(readOnly = true)
    public SeatMapDto getSeatMap(Long showtimeId, Long currentUserId) {
        return seatMapService.getSeatMap(showtimeId, currentUserId);
    }

    public BigDecimal estimateSeatTotal(SeatMapDto seatMap, List<Long> seatIds) {
        return seatMapService.estimateSeatTotal(seatMap, seatIds);
    }

    private Movie resolveMovie(ShowtimeCreateRequest request) {
        if (request.getTmdbId() != null) {
            return movieRepository.findByTmdbId(request.getTmdbId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST,
                            "Chưa có phim rạp với TMDB id=" + request.getTmdbId()
                                    + ". Import phim từ TMDB trước khi tạo suất chiếu."));
        }
        if (request.getMovieId() != null) {
            return movieRepository.findById(request.getMovieId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Phim không tồn tại"));
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cần TMDB id hoặc movie id");
    }

    private static String resolveAgeLabel(ShowtimeBrowseDto first) {
        if (first.getAgeLabel() != null && !first.getAgeLabel().isBlank()) {
            return first.getAgeLabel();
        }
        return MovieAgeUtil.extractAgeLabel(first.getMovieTitle());
    }

    private ShowtimeBrowseDto toBrowseDto(Showtime s) {
        return toBrowseDto(s, movieDisplayService.resolveTitle(s.getMovie(), AppLanguage.VI_VN));
    }

    private ShowtimeBrowseDto toBrowseDto(Showtime s, String movieTitle) {
        long booked = showtimeSeatRepository.countByShowtimeShowtimeIdAndStatus(
                s.getShowtimeId(), SeatStatus.BOOKED);
        int total = s.getRoom().getTotalSeats();
        boolean soldOut = s.getStatus() == ShowtimeStatus.SOLD_OUT || booked >= total;
        Movie movie = s.getMovie();
        return ShowtimeBrowseDto.builder()
                .showtimeId(s.getShowtimeId())
                .movieId(movie.getMovieId())
                .tmdbId(movie.getTmdbId())
                .ageLabel(movie.getAgeLabel())
                .movieTitle(movieTitle)
                .roomName(s.getRoom().getRoomName())
                .startTime(s.getStartTime())
                .basePrice(s.getBasePrice())
                .status(s.getStatus())
                .soldOut(soldOut)
                .availableSeats(total - booked)
                .totalSeats(total)
                .build();
    }
}
