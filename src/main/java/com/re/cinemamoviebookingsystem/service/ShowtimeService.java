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
        LocalDateTime end = start.plusMinutes(movie.getDuration())
                .plusMinutes(cinemaProperties.getCleaningBufferMinutes());

        if (showtimeRepository.existsRoomConflict(room.getRoomId(), start, end, null)) {
            throw new BusinessException(ErrorCode.ROOM_CONFLICT,
                    "Phòng đã có suất chiếu trùng lịch. Vui lòng chọn giờ khác.");
        }

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
        LocalDate today = LocalDate.now();
        List<LocalDate> dayList = IntStream.range(0, 5).mapToObj(today::plusDays).toList();
        LocalDate selected = (selectedDate != null && dayList.contains(selectedDate)) ? selectedDate : today;

        LocalDate finalSelected = selected;
        Map<Long, List<ShowtimeBrowseDto>> grouped = browseUpcoming().stream()
                .filter(s -> s.getTmdbId() != null)
                .filter(s -> s.getStartTime().toLocalDate().equals(finalSelected))
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
            cards.add(ScheduleMovieCardDto.builder()
                    .tmdbId(tmdbId)
                    .movieId(first.getMovieId())
                    .title(first.getMovieTitle())
                    .posterUrl(null)
                    .genresLabel("—")
                    .duration(null)
                    .releaseDate(null)
                    .ageLabel(resolveAgeLabel(first))
                    .ageNote(MovieAgeUtil.buildAgeNote(first.getMovieTitle()))
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
                .movieTitle(movieDisplayService.resolveTitle(showtime.getMovie(), AppLanguage.VI_VN))
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

        if (showtimeRepository.existsRoomConflict(room.getRoomId(), start, end, showtimeId)) {
            throw new BusinessException(ErrorCode.ROOM_CONFLICT, "Phòng trùng lịch với suất khác");
        }

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
        LocalDateTime end = start.plusMinutes(durationMinutes)
                .plusMinutes(cinemaProperties.getCleaningBufferMinutes());
        return showtimeRepository.existsRoomConflict(roomId, start, end, excludeShowtimeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public int bulkCreate(ShowtimeBulkCreateRequest request) {
        Movie movie = resolveMovieForBulk(request);
        List<java.time.LocalTime> slots = parseTimeSlots(request.getTimeSlots());
        if (slots.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Cần ít nhất một khung giờ");
        }
        int created = 0;
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
                }
            }
        }
        return created;
    }

    private Movie resolveMovieForBulk(ShowtimeBulkCreateRequest request) {
        ShowtimeCreateRequest tmp = new ShowtimeCreateRequest();
        tmp.setMovieId(request.getMovieId());
        tmp.setTmdbId(request.getTmdbId());
        return resolveMovie(tmp);
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
            result.add(java.time.LocalTime.parse(part.trim()));
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
                .movieTitle(movieDisplayService.resolveTitle(s.getMovie(), AppLanguage.VI_VN))
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
                .movieTitle(movieDisplayService.resolveTitle(movie, AppLanguage.VI_VN))
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
